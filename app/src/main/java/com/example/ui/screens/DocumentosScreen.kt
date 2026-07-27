package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.ui.viewmodel.WorshipViewModel
import java.text.SimpleDateFormat
import java.util.*

val DOCUMENT_CATEGORIES = listOf(
    "Todos",
    "Estatutos y Legales",
    "Plantillas",
    "Informes y Finanzas",
    "Certificados",
    "Otros"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentosScreen(viewModel: WorshipViewModel) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()

    var selectedCategory by remember { mutableStateOf("Todos") }
    var searchQuery by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var documentToDelete by remember { mutableStateOf<DocumentEntity?>(null) }

    // File Picker Launcher for Multiformat Document Selection
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            showAddDialog = true
        }
    }

    // Filter documents by Category and Search Query
    val filteredDocuments = remember(documents, selectedCategory, searchQuery) {
        documents.filter { doc ->
            val matchesCategory = (selectedCategory == "Todos") || (doc.category == selectedCategory)
            val matchesQuery = searchQuery.isBlank() ||
                    doc.title.contains(searchQuery, ignoreCase = true) ||
                    doc.notes.contains(searchQuery, ignoreCase = true) ||
                    doc.tags.contains(searchQuery, ignoreCase = true) ||
                    doc.fileName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    filePickerLauncher.launch("*/*")
                },
                icon = { Icon(Icons.Default.UploadFile, contentDescription = "Cargar Documento") },
                text = { Text("Cargar Documento", fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF1E3A8A),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("upload_document_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Header Banner Card
            Surface(
                color = Color(0xFF1E3A8A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Documentos Importantes",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Estatutos, plantillas, informes, finanzas y certificados",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF3B82F6).copy(alpha = 0.3f),
                            border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFD700)))
                        ) {
                            Text(
                                text = "${documents.size} archivos",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por título, nota o etiqueta...", fontSize = 13.sp, color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color(0xFF1E3A8A)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("document_search_input")
                    )
                }
            }

            // Categories Horizontal Scrollable Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DOCUMENT_CATEGORIES) { cat ->
                    val isSelected = (cat == selectedCategory)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E3A8A),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF334155)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFFCBD5E1),
                            selectedBorderColor = Color(0xFF1E3A8A)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Documents List or Empty Placeholder
            if (filteredDocuments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != "Todos")
                                "No se encontraron documentos con los filtros aplicados"
                            else "No hay documentos digitalizados en el repositorio",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Seleccionar Archivo")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDocuments, key = { it.id }) { doc ->
                        DocumentItemCard(
                            document = doc,
                            onOpen = { viewModel.openDocument(doc) },
                            onShare = { viewModel.shareDocument(doc) },
                            onDelete = { documentToDelete = doc }
                        )
                    }
                }
            }
        }
    }

    // Add Document Dialog
    if (showAddDialog && selectedFileUri != null) {
        AddDocumentDialog(
            uri = selectedFileUri!!,
            onDismiss = {
                showAddDialog = false
                selectedFileUri = null
            },
            onSave = { title, category, notes, tags ->
                viewModel.saveDocument(title, category, notes, tags, selectedFileUri!!) { success ->
                    if (success) {
                        Toast.makeText(context, "Documento guardado con éxito", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al guardar el documento", Toast.LENGTH_SHORT).show()
                    }
                    showAddDialog = false
                    selectedFileUri = null
                }
            }
        )
    }

    // Confirm Delete Dialog
    if (documentToDelete != null) {
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
            title = { Text("Eliminar Documento", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar permanentemente '${documentToDelete!!.title}' del almacenamiento local?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(documentToDelete!!)
                        Toast.makeText(context, "Documento eliminado", Toast.LENGTH_SHORT).show()
                        documentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun DocumentItemCard(
    document: DocumentEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(document.dateAdded) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(document.dateAdded))
    }

    val categoryColor = when (document.category) {
        "Estatutos y Legales" -> Color(0xFF1E3A8A) // Dark Blue
        "Plantillas" -> Color(0xFF0D9488) // Teal
        "Informes y Finanzas" -> Color(0xFF16A34A) // Green
        "Certificados" -> Color(0xFFD97706) // Amber
        else -> Color(0xFF64748B) // Slate
    }

    val fileIcon: ImageVector = when {
        document.mimeType.contains("pdf", ignoreCase = true) || document.fileName.endsWith(".pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
        document.mimeType.contains("word", ignoreCase = true) || document.fileName.endsWith(".doc", ignoreCase = true) || document.fileName.endsWith(".docx", ignoreCase = true) -> Icons.Default.Description
        document.mimeType.contains("excel", ignoreCase = true) || document.mimeType.contains("sheet", ignoreCase = true) || document.fileName.endsWith(".xls", ignoreCase = true) || document.fileName.endsWith(".xlsx", ignoreCase = true) -> Icons.Default.TableChart
        document.mimeType.contains("image", ignoreCase = true) -> Icons.Default.Image
        else -> Icons.Default.InsertDriveFile
    }

    val fileSizeStr = remember(document.fileSizeBytes) {
        if (document.fileSizeBytes <= 0) ""
        else if (document.fileSizeBytes < 1024) "${document.fileSizeBytes} B"
        else if (document.fileSizeBytes < 1024 * 1024) "${document.fileSizeBytes / 1024} KB"
        else "%.1f MB".format(document.fileSizeBytes / (1024.0 * 1024.0))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Top Row: Category Badge & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = categoryColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = document.category,
                        color = categoryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color(0xFF0D9488), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Details Row
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = categoryColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = fileIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${document.fileName} • ${if (fileSizeStr.isNotEmpty()) "$fileSizeStr • " else ""}$dateStr",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    if (document.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = document.notes,
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (document.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                            Text(
                                text = document.tags,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button to Open Document
            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir / Visualizar Documento", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, notes: String, tags: String) -> Unit
) {
    val context = LocalContext.current

    // Extract default filename from URI
    var defaultName = "Nuevo Documento"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                val foundName = cursor.getString(nameIndex)
                if (!foundName.isNullOrBlank()) {
                    defaultName = foundName.substringBeforeLast('.')
                }
            }
        }
    } catch (_: Exception) {}

    var title by remember { mutableStateOf(defaultName) }
    var category by remember { mutableStateOf("Estatutos y Legales") }
    var notes by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categoriesList = listOf(
        "Estatutos y Legales",
        "Plantillas",
        "Informes y Finanzas",
        "Certificados",
        "Otros"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF1E3A8A))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cargar Nuevo Documento", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre / Título del Documento") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoriesList.forEach { catOption ->
                            DropdownMenuItem(
                                text = { Text(catOption) },
                                onClick = {
                                    category = catOption
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Descripción (Opcional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Etiquetas (Ej: 2026, Acta, Finanzas)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Por favor asigna un título al documento", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(title, category, notes, tags)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Text("Guardar Documento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
