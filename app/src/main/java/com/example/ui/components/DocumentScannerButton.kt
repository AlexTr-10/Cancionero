package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.util.OcrScannerHelper
import kotlinx.coroutines.launch
import java.io.File
import android.graphics.BitmapFactory

@Composable
fun DocumentScannerButton(
    buttonText: String = "Escanear / Foto Documento",
    attachedImagePath: String = "",
    onScanResult: (scannedText: String, savedImagePath: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showOptionsDialog by remember { mutableStateOf(false) }
    var isProcessingOcr by remember { mutableStateOf(false) }
    var showPreviewImageDialog by remember { mutableStateOf(false) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null && pendingCameraFile != null) {
            isProcessingOcr = true
            scope.launch {
                val savedPath = pendingCameraFile!!.absolutePath
                val result = OcrScannerHelper.processImageForText(context, pendingCameraUri!!)
                isProcessingOcr = false
                result.fold(
                    onSuccess = { extractedText ->
                        if (extractedText.isBlank()) {
                            Toast.makeText(context, "Documento digitalizado, pero no se detectó texto legible.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Texto extraído mediante OCR con éxito", Toast.LENGTH_SHORT).show()
                        }
                        onScanResult(extractedText, savedPath)
                    },
                    onFailure = { err ->
                        Toast.makeText(context, "Foto guardada. Error al procesar OCR: ${err.message}", Toast.LENGTH_SHORT).show()
                        onScanResult("", savedPath)
                    }
                )
            }
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val (uri, file) = OcrScannerHelper.createTemporaryCameraUri(context)
            pendingCameraUri = uri
            pendingCameraFile = file
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para escanear documentos.", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingOcr = true
            scope.launch {
                try {
                    val savedPath = OcrScannerHelper.saveImageToInternalStorage(context, uri)
                    val result = OcrScannerHelper.processImageForText(context, uri)
                    isProcessingOcr = false
                    result.fold(
                        onSuccess = { extractedText ->
                            if (extractedText.isBlank()) {
                                Toast.makeText(context, "Imagen guardada, pero no se detectó texto.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Texto extraído mediante OCR con éxito", Toast.LENGTH_SHORT).show()
                            }
                            onScanResult(extractedText, savedPath)
                        },
                        onFailure = { err ->
                            Toast.makeText(context, "Imagen guardada. Error OCR: ${err.message}", Toast.LENGTH_SHORT).show()
                            onScanResult("", savedPath)
                        }
                    )
                } catch (e: Exception) {
                    isProcessingOcr = false
                    Toast.makeText(context, "Error al procesar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showOptionsDialog = true },
                enabled = !isProcessingOcr,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)), // Teal
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("scan_document_button")
            ) {
                if (isProcessingOcr) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escaneando OCR...", fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(buttonText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (attachedImagePath.isNotBlank()) {
                OutlinedButton(
                    onClick = { showPreviewImageDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0D9488)),
                    border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0D9488)))
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Ver Documento", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver Foto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (attachedImagePath.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Documento original adjuntado",
                    fontSize = 11.sp,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Source Options Dialog
    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color(0xFF0D9488))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escanear Documento (OCR)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Elige cómo deseas capturar el documento para digitalizarlo y extraer su texto automáticamente:")
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showOptionsDialog = false
                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                val (uri, file) = OcrScannerHelper.createTemporaryCameraUri(context)
                                pendingCameraUri = uri
                                pendingCameraFile = file
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tomar Foto con Cámara")
                    }

                    OutlinedButton(
                        onClick = {
                            showOptionsDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir de la Galería")
                    }

                    TextButton(
                        onClick = { showOptionsDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    // Preview Image Dialog
    if (showPreviewImageDialog && attachedImagePath.isNotBlank()) {
        Dialog(onDismissRequest = { showPreviewImageDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Documento Digitalizado",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                        IconButton(onClick = { showPreviewImageDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val file = File(attachedImagePath)
                    if (file.exists()) {
                        val bitmap = remember(attachedImagePath) {
                            BitmapFactory.decodeFile(file.absolutePath)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Foto del documento",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                            )
                        } else {
                            Text("No se pudo cargar la imagen.", color = Color.Red, fontSize = 13.sp)
                        }
                    } else {
                        Text("El archivo de imagen no existe en la ruta: $attachedImagePath", color = Color.Red, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showPreviewImageDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}
