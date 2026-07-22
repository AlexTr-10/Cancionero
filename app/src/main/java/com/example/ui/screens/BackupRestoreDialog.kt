package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.DropshipViewModel
import com.example.util.BackupResult
import com.example.util.RestoreResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreDialog(
    viewModel: DropshipViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isBackupInProgress by viewModel.isBackupInProgress.collectAsStateWithLifecycle()
    val isRestoreInProgress by viewModel.isRestoreInProgress.collectAsStateWithLifecycle()

    var backupResult by remember { mutableStateOf<BackupResult?>(null) }
    var restoreResult by remember { mutableStateOf<RestoreResult?>(null) }
    var replaceOnRestore by remember { mutableStateOf(true) }

    val openZipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackupZip(uri, replaceExisting = replaceOnRestore) { result ->
                restoreResult = result
                if (result.isSuccess) {
                    Toast.makeText(
                        context,
                        "¡Restauración exitosa! Se recuperaron ${result.catalogProductsRestored} productos y ${result.photosRestored} fotos.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        result.errorMessage ?: "Error durante la restauración.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val saveZipDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { destUri ->
        if (destUri != null && backupResult?.zipFile != null) {
            val success = viewModel.copyZipToUri(backupResult!!.zipFile!!, destUri)
            if (success) {
                Toast.makeText(context, "Respaldo guardado correctamente en tu almacenamiento.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "No se pudo guardar el archivo de respaldo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF0284C7), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White)
                }
                Column {
                    Text("Copia de Seguridad & Restauración", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Respalda y migra todo en un archivo ZIP", fontSize = 11.5.sp, color = Color.Gray)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info Banner
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📦 Tu respaldo ZIP incluirá:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Catálogo completo de productos con fichas y variaciones", fontSize = 11.5.sp, color = Color(0xFF475569))
                            Text("• Galería de fotografías e imágenes de productos y empresa", fontSize = 11.5.sp, color = Color(0xFF475569))
                            Text("• Datos de la empresa, contacto y eslogan", fontSize = 11.5.sp, color = Color(0xFF475569))
                            Text("• Historial de simulaciones, márgenes, ranking y checklist", fontSize = 11.5.sp, color = Color(0xFF475569))
                        }
                    }
                }

                // Section 1: CREATE BACKUP
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                                Text("1. Crear Copia de Seguridad", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Empaca todos tus datos y fotos en un único archivo ZIP para guardarlo o enviarlo por WhatsApp, Correo o Google Drive.", fontSize = 11.5.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isBackupInProgress) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFF0284C7))
                                    Text("Generando archivo ZIP de respaldo...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.createBackupZip { res ->
                                            backupResult = res
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("create_backup_btn")
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generar Respaldo (.ZIP)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            // Backup Success Box
                            backupResult?.let { res ->
                                Spacer(modifier = Modifier.height(10.dp))
                                if (res.isSuccess && res.zipFile != null) {
                                    Surface(
                                        color = Color(0xFFF0FDF4),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                                Text("¡Respaldo ZIP generado con éxito!", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF15803D))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("• Fichero: ${res.zipFile.name}", fontSize = 11.sp, color = Color(0xFF166534))
                                            Text("• Productos: ${res.catalogProductsCount} | Fotos: ${res.photosCount} | Simulaciones: ${res.simulationsCount}", fontSize = 11.sp, color = Color(0xFF166534))

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        viewModel.shareBackupZip(res.zipFile)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("share_backup_btn"),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Compartir", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        saveZipDocumentLauncher.launch(res.zipFile.name)
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("save_backup_btn"),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Guardar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text("Error: ${res.errorMessage}", color = Color.Red, fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }

                // Section 2: RESTORE BACKUP
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Unarchive, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                                Text("2. Restaurar Copia de Seguridad", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Selecciona un archivo ZIP generado anteriormente para recuperar tus datos y fotografías en este dispositivo.", fontSize = 11.5.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.height(8.dp))

                            // Mode selection
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = replaceOnRestore,
                                    onClick = { replaceOnRestore = true },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF7C3AED))
                                )
                                Text("Reemplazar todo (Recomendado)", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = !replaceOnRestore,
                                    onClick = { replaceOnRestore = false },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF7C3AED))
                                )
                                Text("Combinar con datos actuales", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isRestoreInProgress) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFF7C3AED))
                                    Text("Descomprimiendo y restaurando datos...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                                }
                            } else {
                                Button(
                                    onClick = {
                                        openZipPickerLauncher.launch("*/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("restore_backup_btn")
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Seleccionar Archivo .ZIP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            // Restore Result
                            restoreResult?.let { res ->
                                Spacer(modifier = Modifier.height(10.dp))
                                if (res.isSuccess) {
                                    Surface(
                                        color = Color(0xFFF0FDF4),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                                Text("¡Restauración completada con éxito!", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF15803D))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("• Productos en catálogo: ${res.catalogProductsRestored}", fontSize = 11.sp, color = Color(0xFF166534))
                                            Text("• Fotografías recuperadas: ${res.photosRestored}", fontSize = 11.sp, color = Color(0xFF166534))
                                            Text("• Simulaciones y cálculos: ${res.simulationsRestored}", fontSize = 11.sp, color = Color(0xFF166534))
                                        }
                                    }
                                } else {
                                    Text("Error de restauración: ${res.errorMessage}", color = Color.Red, fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
