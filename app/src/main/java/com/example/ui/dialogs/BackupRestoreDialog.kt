package com.example.ui.dialogs

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.BackupSummaryStats
import com.example.ui.viewmodel.WorshipViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreDialog(
    viewModel: WorshipViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    var selectedUriForImport by remember { mutableStateOf<Uri?>(null) }
    var showConfirmImportDialog by remember { mutableStateOf(false) }
    var importSuccessStats by remember { mutableStateOf<BackupSummaryStats?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // SAF File Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUriForImport = uri
            showConfirmImportDialog = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E3A8A).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color(0xFF1E3A8A)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Copia de Seguridad",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Respaldo y Restauración de datos",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Export
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Exportar Copia de Seguridad",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Genera un archivo .json completo con todas las Actas, Documentos, Boletines, Cronograma, Canciones y Ajustes de Room.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isExporting = true
                                viewModel.exportBackup(
                                    onSuccess = { file ->
                                        isExporting = false
                                        Toast.makeText(context, "Respaldo generado correctamente", Toast.LENGTH_SHORT).show()
                                        viewModel.shareBackupFile(file)
                                    },
                                    onError = { err ->
                                        isExporting = false
                                        errorMessage = err
                                    }
                                )
                            },
                            enabled = !isExporting && !isImporting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("export_backup_button")
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generando Respaldo...")
                            } else {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exportar y Compartir (WhatsApp / Drive)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Import / Restore
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Color(0xFF1E3A8A),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Restaurar Copia de Seguridad",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Selecciona un archivo de respaldo (.json) guardado previamente en la memoria del dispositivo.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            },
                            enabled = !isExporting && !isImporting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("import_backup_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Seleccionar Archivo de Respaldo", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }

    // Confirmation Alert before Overwriting
    if (showConfirmImportDialog && selectedUriForImport != null) {
        AlertDialog(
            onDismissRequest = { showConfirmImportDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("¿Restaurar Copia de Seguridad?") },
            text = {
                Text(
                    "Esta acción REEMPLAZARÁ la base de datos actual con la información del archivo de respaldo seleccionado.\n\n" +
                            "Se restaurarán Actas de Reunión, Documentos Importantes, Boletines, Cronograma y Canciones. ¿Deseas continuar?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmImportDialog = false
                        isImporting = true
                        viewModel.importBackup(
                            uri = selectedUriForImport!!,
                            onSuccess = { stats ->
                                isImporting = false
                                importSuccessStats = stats
                                Toast.makeText(context, "Restauración completada con éxito", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                isImporting = false
                                errorMessage = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("Sí, Restaurar Datos")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmImportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Success Summary Dialog
    importSuccessStats?.let { stats ->
        AlertDialog(
            onDismissRequest = { importSuccessStats = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF15803D),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Restauración Exitosa", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Se han restaurado correctamente los siguientes elementos en Room:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Actas de Reunión: ${stats.actasCount}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("• Documentos Importantes: ${stats.documentsCount}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("• Canciones de Púlpito: ${stats.songsCount}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("• Mosaicos: ${stats.mosaicsCount}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("• Órdenes de Púlpito: ${stats.commandsCount}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("• Historial de Boletines: ${stats.bulletinsCount}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("• Cronograma Anual: ${stats.scheduleCount}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { importSuccessStats = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Error Dialog
    errorMessage?.let { err ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("Error") },
            text = { Text(err) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("Aceptar")
                }
            }
        )
    }
}
