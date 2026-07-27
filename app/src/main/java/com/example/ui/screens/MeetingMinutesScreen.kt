package com.example.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MeetingCommitment
import com.example.data.model.MeetingMinute
import com.example.ui.components.DocumentScannerButton
import com.example.ui.viewmodel.WorshipViewModel
import com.example.util.DocxExporter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingMinutesScreen(viewModel: WorshipViewModel) {
    val context = LocalContext.current
    val minutesList by viewModel.meetingMinutes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("Todos") }

    var showFormDialog by remember { mutableStateOf(false) }
    var editingMinute by remember { mutableStateOf<MeetingMinute?>(null) }
    var previewMinute by remember { mutableStateOf<MeetingMinute?>(null) }
    var minuteToDelete by remember { mutableStateOf<MeetingMinute?>(null) }

    val meetingTypes = listOf("Todos", "Ordinaria", "Extraordinaria", "Asamblea", "Comité", "Junta Directiva")

    val filteredList = remember(minutesList, searchQuery, selectedTypeFilter) {
        minutesList.filter { minute ->
            val matchesSearch = minute.folioNumber.contains(searchQuery, ignoreCase = true) ||
                    minute.location.contains(searchQuery, ignoreCase = true) ||
                    minute.agenda.contains(searchQuery, ignoreCase = true) ||
                    minute.meetingType.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedTypeFilter == "Todos" || minute.meetingType.equals(selectedTypeFilter, ignoreCase = true)
            matchesSearch && matchesType
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)), // Deep Blue
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Actas de Reunión",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Gestión y exportación nativa a Word (.docx)",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por folio, lugar o contenido...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("search_actas_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.height(36.dp)) { } // dummy or Row
                        ScrollableChipRow(
                            types = meetingTypes,
                            selectedType = selectedTypeFilter,
                            onTypeSelected = { selectedTypeFilter = it }
                        )
                    }
                }
            }

            // List of Actas
            if (filteredList.isEmpty()) {
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
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedTypeFilter != "Todos") "No se encontraron actas con los filtros aplicados." else "No hay actas de reunión registradas.",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                editingMinute = null
                                showFormDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Crear Primera Acta")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { minute ->
                        MeetingMinuteCard(
                            minute = minute,
                            onEdit = {
                                editingMinute = minute
                                showFormDialog = true
                            },
                            onPreview = {
                                previewMinute = minute
                            },
                            onExportDocx = {
                                try {
                                    val docxFile = DocxExporter.generateDocx(context, minute)
                                    Toast.makeText(context, "Documento Word generado: ${docxFile.name}", Toast.LENGTH_SHORT).show()
                                    DocxExporter.shareDocx(context, docxFile, minute.folioNumber)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error generando Word: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            onDelete = {
                                minuteToDelete = minute
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // FAB to create new Acta
        FloatingActionButton(
            onClick = {
                editingMinute = null
                showFormDialog = true
            },
            containerColor = Color(0xFF1E3A8A),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("create_acta_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Acta")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva Acta", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Create / Edit Form Dialog
    if (showFormDialog) {
        MeetingMinuteFormDialog(
            initialMinute = editingMinute,
            onDismiss = { showFormDialog = false },
            onSave = { updatedMinute ->
                viewModel.saveMeetingMinute(updatedMinute)
                showFormDialog = false
                Toast.makeText(context, "Acta guardada correctamente", Toast.LENGTH_SHORT).show()
            },
            onExportDirect = { minuteToExport ->
                viewModel.saveMeetingMinute(minuteToExport)
                showFormDialog = false
                try {
                    val file = DocxExporter.generateDocx(context, minuteToExport)
                    DocxExporter.shareDocx(context, file, minuteToExport.folioNumber)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al generar Word: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Preview Dialog
    previewMinute?.let { minute ->
        MeetingMinutePreviewDialog(
            minute = minute,
            onDismiss = { previewMinute = null },
            onExportWord = {
                try {
                    val file = DocxExporter.generateDocx(context, minute)
                    DocxExporter.shareDocx(context, file, minute.folioNumber)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Delete confirmation dialog
    minuteToDelete?.let { minute ->
        AlertDialog(
            onDismissRequest = { minuteToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
            title = { Text("¿Eliminar Acta?") },
            text = { Text("¿Estás seguro de que deseas eliminar el Acta ${minute.folioNumber.ifBlank { "sin folio" }}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMeetingMinute(minute.id)
                        minuteToDelete = null
                        Toast.makeText(context, "Acta eliminada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { minuteToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ScrollableChipRow(
    types: List<String>,
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { type ->
            val isSelected = type.equals(selectedType, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onTypeSelected(type) },
                label = { Text(type, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFD700),
                    selectedLabelColor = Color(0xFF1E3A8A),
                    containerColor = Color.White.copy(alpha = 0.15f),
                    labelColor = Color.White
                ),
                border = null
            )
        }
    }
}

@Composable
fun MeetingMinuteCard(
    minute: MeetingMinute,
    onEdit: () -> Unit,
    onPreview: () -> Unit,
    onExportDocx: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF1E3A8A).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = minute.meetingType,
                            color = Color(0xFF1E3A8A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (minute.folioNumber.isNotBlank()) minute.folioNumber else "Sin Folio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E293B)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = minute.dateTime.ifBlank { "Fecha no especificada" },
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = minute.location.ifBlank { "Sede no especificada" },
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (minute.agenda.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Orden del Día: ${minute.agenda}",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (minute.commitments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "✓ ${minute.commitments.size} compromiso(s) acordado(s)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF15803D)
                )
            }

            if (minute.attachedImagePath.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E3A8A).copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(14.dp))
                    Text(
                        text = "Documento Digitalizado Adjunto",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onPreview,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onExportDocx,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)), // Green for Word
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Word (.docx) / WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingMinuteFormDialog(
    initialMinute: MeetingMinute?,
    onDismiss: () -> Unit,
    onSave: (MeetingMinute) -> Unit,
    onExportDirect: (MeetingMinute) -> Unit
) {
    val context = LocalContext.current

    var folioNumber by remember { mutableStateOf(initialMinute?.folioNumber ?: "Acta N° 001 - ${Calendar.getInstance().get(Calendar.YEAR)}") }
    var dateTime by remember {
        mutableStateOf(
            initialMinute?.dateTime ?: SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )
    }
    var location by remember { mutableStateOf(initialMinute?.location ?: "") }
    var meetingType by remember { mutableStateOf(initialMinute?.meetingType ?: "Ordinaria") }
    var attendees by remember { mutableStateOf(initialMinute?.attendees ?: "") }
    var absentees by remember { mutableStateOf(initialMinute?.absentees ?: "") }
    var agenda by remember { mutableStateOf(initialMinute?.agenda ?: "") }
    var discussion by remember { mutableStateOf(initialMinute?.discussion ?: "") }
    var secretary by remember { mutableStateOf(initialMinute?.secretary ?: "") }
    var president by remember { mutableStateOf(initialMinute?.president ?: "") }
    var attachedImagePath by remember { mutableStateOf(initialMinute?.attachedImagePath ?: "") }

    val commitments = remember { mutableStateListOf<MeetingCommitment>().apply {
        if (initialMinute?.commitments != null && initialMinute.commitments.isNotEmpty()) {
            addAll(initialMinute.commitments)
        }
    } }

    val meetingTypes = listOf("Ordinaria", "Extraordinaria", "Asamblea", "Comité", "Junta Directiva")
    var expandedTypeDropdown by remember { mutableStateOf(false) }

    // Date / Time Pickers state
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Calendar helper
    val calendar = remember { Calendar.getInstance() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                TopAppBar(
                    title = {
                        Text(
                            text = if (initialMinute == null) "Nueva Acta de Reunión" else "Editar Acta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val created = MeetingMinute(
                                    id = initialMinute?.id ?: UUID.randomUUID().toString(),
                                    folioNumber = folioNumber,
                                    dateTime = dateTime,
                                    location = location,
                                    meetingType = meetingType,
                                    attendees = attendees,
                                    absentees = absentees,
                                    agenda = agenda,
                                    discussion = discussion,
                                    commitments = commitments.toList(),
                                    secretary = secretary,
                                    president = president,
                                    attachedImagePath = attachedImagePath,
                                    createdAt = initialMinute?.createdAt ?: System.currentTimeMillis()
                                )
                                onSave(created)
                            }
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF1F5F9))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Datos Generales
                    Text("INFORMACIÓN GENERAL", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A8A))

                    OutlinedTextField(
                        value = folioNumber,
                        onValueChange = { folioNumber = it },
                        label = { Text("Número / Folio de Acta *") },
                        placeholder = { Text("Ej: Acta N° 001 - 2026") },
                        modifier = Modifier.fillMaxWidth().testTag("folio_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dateTime,
                            onValueChange = { dateTime = it },
                            label = { Text("Fecha y Hora") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePickerDialog = true }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Meeting Type Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedTypeDropdown,
                            onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = meetingType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de Reunión") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedTypeDropdown,
                                onDismissRequest = { expandedTypeDropdown = false }
                            ) {
                                meetingTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            meetingType = type
                                            expandedTypeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Lugar / Sede") },
                        placeholder = { Text("Ej: Salón Consistorial / Templo Principal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    HorizontalDivider()

                    // 2. Asistentes y Ausentes
                    Text("2. ASISTENCIA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A8A))

                    OutlinedTextField(
                        value = attendees,
                        onValueChange = { attendees = it },
                        label = { Text("Lista de Asistentes") },
                        placeholder = { Text("Nombres de los asistentes...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = absentees,
                        onValueChange = { absentees = it },
                        label = { Text("Lista de Ausentes") },
                        placeholder = { Text("Nombres de los ausentes o justificados...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    HorizontalDivider()

                    // 3. Orden del Día
                    Text("3. ORDEN DEL DÍA (PUNTOS A TRATAR)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A8A))

                    OutlinedTextField(
                        value = agenda,
                        onValueChange = { agenda = it },
                        label = { Text("Orden del Día") },
                        placeholder = { Text("1. Lectura del acta anterior\n2. Informe tesorería\n3. Asuntos pendientes...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )

                    HorizontalDivider()

                    // 4. Desarrollo / Debate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("4. DESARROLLO / DEBATE DE LA SESIÓN", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A8A))
                    }

                    DocumentScannerButton(
                        buttonText = "Escanear / Foto Documento",
                        attachedImagePath = attachedImagePath,
                        onScanResult = { extractedText, savedPath ->
                            if (extractedText.isNotBlank()) {
                                discussion = if (discussion.isBlank()) extractedText else "$discussion\n\n[Texto Digitalizado OCR]:\n$extractedText"
                            }
                            attachedImagePath = savedPath
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = discussion,
                        onValueChange = { discussion = it },
                        label = { Text("Desarrollo de la Sesión") },
                        placeholder = { Text("Escribe los acuerdos y resumen de las intervenciones...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )

                    HorizontalDivider()

                    // 5. Compromisos y Tareas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("5. COMPROMISOS Y TAREAS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A8A))
                        TextButton(
                            onClick = {
                                commitments.add(MeetingCommitment(agreement = "", responsible = "", dueDate = ""))
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar Tarea")
                        }
                    }

                    if (commitments.isEmpty()) {
                        Text(
                            text = "No se han agregado tareas o acuerdos.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        commitments.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Compromiso #${index + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        IconButton(
                                            onClick = { commitments.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                                        }
                                    }

                                    OutlinedTextField(
                                        value = item.agreement,
                                        onValueChange = { commitments[index] = item.copy(agreement = it) },
                                        label = { Text("Acuerdo / Tarea") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = item.responsible,
                                            onValueChange = { commitments[index] = item.copy(responsible = it) },
                                            label = { Text("Responsable") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = item.dueDate,
                                            onValueChange = { commitments[index] = item.copy(dueDate = it) },
                                            label = { Text("Fecha Límite") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // 6. Firmantes
                    Text("6. FIRMANTES", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A8A))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = secretary,
                            onValueChange = { secretary = it },
                            label = { Text("Secretario/a") },
                            placeholder = { Text("Nombre del Secretario/a") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = president,
                            onValueChange = { president = it },
                            label = { Text("Presidente / Pastor") },
                            placeholder = { Text("Nombre del Pastor / Pres.") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Action Bar
                    Button(
                        onClick = {
                            val currentMinute = MeetingMinute(
                                id = initialMinute?.id ?: UUID.randomUUID().toString(),
                                folioNumber = folioNumber,
                                dateTime = dateTime,
                                location = location,
                                meetingType = meetingType,
                                attendees = attendees,
                                absentees = absentees,
                                agenda = agenda,
                                discussion = discussion,
                                commitments = commitments.toList(),
                                secretary = secretary,
                                president = president,
                                attachedImagePath = attachedImagePath,
                                createdAt = initialMinute?.createdAt ?: System.currentTimeMillis()
                            )
                            onExportDirect(currentMinute)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar y Exportar Word (.docx) / WhatsApp", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Native DatePicker Dialog integration
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = calendar.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedCal = Calendar.getInstance().apply { timeInMillis = millis }
                            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCal.time)
                            val timePart = if (dateTime.contains(" ")) dateTime.substringAfter(" ") else "19:00"
                            dateTime = "$dateStr $timePart"
                        }
                        showDatePickerDialog = false
                        showTimePickerDialog = true
                    }
                ) {
                    Text("Siguiente (Hora)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Native TimePicker Dialog
    if (showTimePickerDialog) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        DisposableEffect(Unit) {
            val timePicker = TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->
                    val datePart = if (dateTime.contains(" ")) dateTime.substringBefore(" ") else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                    dateTime = "$datePart $formattedTime"
                    showTimePickerDialog = false
                },
                hour,
                minute,
                true
            )
            timePicker.setOnCancelListener { showTimePickerDialog = false }
            timePicker.show()
            onDispose { timePicker.dismiss() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingMinutePreviewDialog(
    minute: MeetingMinute,
    onDismiss: () -> Unit,
    onExportWord: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Vista Previa del Acta", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF1F5F9))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ACTA DE REUNIÓN",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E3A8A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = minute.folioNumber.ifBlank { "Sin Folio" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Details Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Fecha y Hora: ${minute.dateTime.ifBlank { "N/A" }}", fontSize = 13.sp)
                            Text("Lugar / Sede: ${minute.location.ifBlank { "N/A" }}", fontSize = 13.sp)
                            Text("Tipo: ${minute.meetingType}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        }
                    }

                    Text("1. ASISTENTES Y AUSENTES", fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    Text("Asistentes: ${minute.attendees.ifBlank { "N/A" }}", fontSize = 13.sp)
                    Text("Ausentes: ${minute.absentees.ifBlank { "N/A" }}", fontSize = 13.sp)

                    HorizontalDivider()

                    Text("2. ORDEN DEL DÍA", fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    Text(minute.agenda.ifBlank { "N/A" }, fontSize = 13.sp)

                    HorizontalDivider()

                    Text("3. DESARROLLO / DEBATE DE LA SESIÓN", fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    Text(minute.discussion.ifBlank { "N/A" }, fontSize = 13.sp)

                    HorizontalDivider()

                    Text("4. COMPROMISOS Y TAREAS", fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    if (minute.commitments.isEmpty()) {
                        Text("Sin compromisos registrados.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        minute.commitments.forEachIndexed { i, c ->
                            Text("${i + 1}. ${c.agreement} (Resp: ${c.responsible.ifBlank { "N/A" }} | Lim: ${c.dueDate.ifBlank { "N/A" }})", fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider()

                    Text("5. FIRMANTES", fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("________________________")
                            Text(minute.secretary.ifBlank { "Secretario/a" }, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("________________________")
                            Text(minute.president.ifBlank { "Presidente / Pastor" }, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = onExportWord,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar Word (.docx) y Compartir", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
