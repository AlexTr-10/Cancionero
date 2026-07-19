package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AnnualScheduleItem
import com.example.data.model.BulletinDay
import com.example.data.model.WeeklyBulletin
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.WorshipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationWrapper(viewModel: WorshipViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Scaffold(
        topBar = {
            if (currentScreen != Screen.ModoPulpito) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Secretaría Eclesiástica ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "- Boletín y Anuncios",
                                color = Color(0xFFFFD700), // Dorado brillante
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1E3A8A) // Azul Rey fijo
                    )
                )
            }
        },
        bottomBar = {
            if (currentScreen != Screen.ModoPulpito) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Boletin,
                        onClick = { viewModel.navigateTo(Screen.Boletin) },
                        icon = { Icon(Icons.Default.Book, contentDescription = "Boletín") },
                        label = { Text("Boletín Semanal") },
                        modifier = Modifier.testTag("nav_boletin")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Cronograma,
                        onClick = { viewModel.navigateTo(Screen.Cronograma) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Cronograma") },
                        label = { Text("Cronograma") },
                        modifier = Modifier.testTag("nav_cronograma")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.ModoPulpito,
                        onClick = { viewModel.navigateTo(Screen.ModoPulpito) },
                        icon = { Icon(Icons.Default.CoPresent, contentDescription = "Púlpito") },
                        label = { Text("Modo Púlpito") },
                        modifier = Modifier.testTag("nav_pulpito")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Boletin -> BoletinScreen(viewModel)
                    Screen.Cronograma -> CronogramaScreen(viewModel)
                    Screen.ModoPulpito -> PulpitoScreen(viewModel)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PANTALLA: BOLETÍN SEMANAL (ENTRADA DE DATOS)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoletinScreen(viewModel: WorshipViewModel) {
    val bulletin by viewModel.bulletin.collectAsState()
    val bulletinHistory by viewModel.bulletinHistory.collectAsState()
    val context = LocalContext.current

    var selectedDayIndex by remember { mutableStateOf(0) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título de la pantalla y botón de Historial
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Boletín Semanal",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E3A8A)
                )
                
                Button(
                    onClick = { showHistoryDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = "Historial", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Historial", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // 1. Datos Generales del Boletín (Rango de fecha y Comité) - Tarjeta Blanca con Sombra Suave
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "Información General",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color(0xFF1E3A8A) // Azul Rey
                        )
                    }

                    OutlinedTextField(
                        value = bulletin.dateRange,
                        onValueChange = { viewModel.updateDateRange(it) },
                        label = { Text("Rango de Fechas") },
                        placeholder = { Text("Ej: 20 al 26 de Julio") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_rango_fechas"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bulletin.committee,
                        onValueChange = { viewModel.updateCommittee(it) },
                        label = { Text("Comité / Grupo a Cargo") },
                        placeholder = { Text("Ej: Comité de Damas") },
                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_comite_cargo"),
                        singleLine = true
                    )
                }
            }
        }

        // 2. Servicios y Asignaciones Diarias - Tarjeta Blanca con Sombra Suave
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "Servicios y Asignaciones Diarias",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }

                    // Selector de días con scroll lateral
                    ScrollableTabRow(
                        selectedTabIndex = selectedDayIndex,
                        edgePadding = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        containerColor = Color(0xFFF1F5F9), // Muy claro
                        contentColor = Color(0xFF1E3A8A)
                    ) {
                        bulletin.days.forEachIndexed { index, day ->
                            Tab(
                                selected = selectedDayIndex == index,
                                onClick = { selectedDayIndex = index },
                                text = { 
                                    Text(
                                        text = day.dayName, 
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDayIndex == index) Color(0xFFCA8A04) else Color(0xFF1E293B)
                                    ) 
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val day = bulletin.days[selectedDayIndex]

                    // Información del día seleccionado
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Asignación: ${day.dayName}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E3A8A)
                        )
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null,
                            tint = Color(0xFFCA8A04) // Accent Gold
                        )
                    }

                    // Campos de Entrada para el Día Seleccionado
                    // 1. Culto / Servicio
                    OutlinedTextField(
                        value = day.serviceName,
                        onValueChange = { viewModel.updateDayField(selectedDayIndex, "serviceName", it) },
                        label = { Text("Culto / Servicio") },
                        placeholder = { Text("Ej: Culto de Adoración") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 2. Hora
                    OutlinedTextField(
                        value = day.time,
                        onValueChange = { viewModel.updateDayField(selectedDayIndex, "time", it) },
                        label = { Text("Hora") },
                        placeholder = { Text("Ej: 19:30") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 3. Alabanza
                    OutlinedTextField(
                        value = day.worshipTeam,
                        onValueChange = { viewModel.updateDayField(selectedDayIndex, "worshipTeam", it) },
                        label = { Text("Alabanza") },
                        placeholder = { Text("Ej: Grupo Eben-Ezer") },
                        leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 4. Ujieres
                    OutlinedTextField(
                        value = day.ushers,
                        onValueChange = { viewModel.updateDayField(selectedDayIndex, "ushers", it) },
                        label = { Text("Ujieres") },
                        placeholder = { Text("Ej: Hno. Juan y Hna. Rosa") },
                        leadingIcon = { Icon(Icons.Default.VolunteerActivism, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 5. Uniforme de Ujieres (con ícono representativo de gancho/ropa) inmediatamente después de Ujieres
                    OutlinedTextField(
                        value = day.notesUniform,
                        onValueChange = { viewModel.updateDayField(selectedDayIndex, "notesUniform", it) },
                        label = { Text("Uniforme de Ujieres") },
                        placeholder = { Text("Ej: Uniforme Oficial Azul / Traje Oscuro") },
                        leadingIcon = { Icon(Icons.Default.Checkroom, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 6. DECOM (Multimedia)
                    OutlinedTextField(
                        value = day.decom,
                        onValueChange = { viewModel.updateDayField(selectedDayIndex, "decom", it) },
                        label = { Text("DECOM (Multimedia)") },
                        placeholder = { Text("Ej: Hno. Mateo") },
                        leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 7. Sonido
                    OutlinedTextField(
                        value = day.sound,
                        onValueChange = { viewModel.updateDayField(selectedDayIndex, "sound", it) },
                        label = { Text("Sonido") },
                        placeholder = { Text("Ej: Hno. Marcos") },
                        leadingIcon = { Icon(Icons.Default.SettingsVoice, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // 3. Anuncios Generales y Avisos - Tarjeta Blanca con Sombra Suave
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "Anuncios Generales y Avisos",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }

                    OutlinedTextField(
                        value = bulletin.generalAnnouncements,
                        onValueChange = { viewModel.updateGeneralAnnouncements(it) },
                        placeholder = { Text("Ingresa los anuncios importantes de la semana aquí...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("input_anuncios"),
                        maxLines = 10
                    )
                }
            }
        }

        // Botones de Acción (Guardar Cambios Destacado, Exportar PDF, Compartir Whatsapp)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // BOTÓN DORADO DESTACADO: GUARDAR CAMBIOS DE LA SEMANA
                Button(
                    onClick = {
                        viewModel.saveCurrentToHistory()
                        Toast.makeText(context, "¡Boletín semanal guardado con éxito!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_guardar_boletin"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCA8A04), // Dorado / Oro
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Cambios de la Semana", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
                }

                // Generar PDF
                Button(
                    onClick = { printPdf(context, bulletin) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_generar_pdf"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)), // Azul Rey
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generar PDF en 1 Hoja", fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Compartir Whatsapp
                Button(
                    onClick = { shareOnWhatsApp(context, bulletin) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_compartir_whatsapp"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartir en WhatsApp (Copiar y Enviar)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // Modal del Historial de Boletines
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historial",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A),
                        fontSize = 18.sp
                    )
                    
                    Button(
                        onClick = {
                            viewModel.createNewBulletin()
                            showHistoryDialog = false
                            Toast.makeText(context, "Nuevo boletín semanal iniciado", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCA8A04)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nuevo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Selecciona un boletín guardado para cargarlo en el editor principal:",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (bulletinHistory.isEmpty()) {
                                item {
                                    Text(
                                        text = "No hay boletines guardados en el historial.",
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                items(bulletinHistory) { b ->
                                    val isCurrent = bulletin.id == b.id
                                    val cardBg = if (isCurrent) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                                    val cardBorder = if (isCurrent) Color(0xFF1E3A8A) else Color(0xFFE2E8F0)
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cardBg)
                                            .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.loadBulletinFromHistory(b)
                                                showHistoryDialog = false
                                                Toast.makeText(context, "Cargado: ${b.dateRange}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = b.dateRange.ifBlank { "Semana sin fecha" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isCurrent) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                                            )
                                            if (b.committee.isNotBlank()) {
                                                Text(
                                                    text = "Comité: ${b.committee}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Activo",
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteBulletinFromHistory(b.id)
                                                    Toast.makeText(context, "Boletín eliminado", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Eliminar",
                                                    tint = Color.Red.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Cerrar", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// PANTALLA: CRONOGRAMA ANUAL
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CronogramaScreen(viewModel: WorshipViewModel) {
    val scheduleList by viewModel.scheduleList.collectAsState()
    val committees by viewModel.committees.collectAsState()
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var showManageCommitteesDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<AnnualScheduleItem?>(null) }

    val months = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    val currentMonthIndex = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    var selectedMonthIndex by remember { mutableStateOf(currentMonthIndex) }

    var dateState by remember { mutableStateOf("") }
    var committeeState by remember { mutableStateOf("") }
    var descriptionState by remember { mutableStateOf("") }
    var monthState by remember { mutableStateOf(months[currentMonthIndex]) }

    var searchQuery by remember { mutableStateOf("") }
    val committeeFilters = listOf("Todos") + committees
    var selectedFilterCommittee by remember { mutableStateOf("Todos") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val jsonStr = viewModel.exportBackup()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(jsonStr.toByteArray())
                }
                Toast.makeText(context, "¡Respaldo exportado con éxito!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes()
                    val jsonStr = String(bytes)
                    val success = viewModel.importBackup(jsonStr)
                    if (success) {
                        Toast.makeText(context, "¡Copia de seguridad restaurada con éxito!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "El archivo de respaldo no es válido", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al importar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun getMonthOfItem(item: AnnualScheduleItem, default: String): String {
        if (item.month.isNotBlank()) return item.month
        val found = months.firstOrNull { item.date.contains(it, ignoreCase = true) }
        return found ?: default
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItem = null
                    monthState = months[selectedMonthIndex]
                    dateState = ""
                    committeeState = ""
                    descriptionState = ""
                    showDialog = true
                },
                containerColor = Color(0xFFCA8A04), // Dorado
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_schedule")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Fecha", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC)) // Fondo slate sutil
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cronograma Anual",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E3A8A)
                )
                
                // Botón de Copia de Seguridad
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            val jsonStr = viewModel.exportBackup()
                            exportLauncher.launch("respaldo_boletin_iglesia.json")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E3A8A).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Exportar Copia de Seguridad",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFCA8A04).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Importar Copia de Seguridad",
                            tint = Color(0xFFCA8A04),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 1. Selector de Meses (Barra horizontal deslizable con fondo Azul Rey para seleccionado)
            Text(
                text = "Seleccionar Mes:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(months.size) { index ->
                    val monthName = months[index]
                    val isSelected = selectedMonthIndex == index
                    val backgroundColor = if (isSelected) Color(0xFF1E3A8A) else Color(0xFFE2E8F0)
                    val contentColor = if (isSelected) Color.White else Color(0xFF334155)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(backgroundColor)
                            .clickable { selectedMonthIndex = index }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = monthName,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = contentColor
                        )
                    }
                }
            }

            // 3. Filtro por Comité y Campo de Búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar evento o comité...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E3A8A),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(committeeFilters.size) { idx ->
                        val filterName = committeeFilters[idx]
                        val isSelected = selectedFilterCommittee == filterName
                        val chipBg = if (isSelected) Color(0xFFCA8A04) else Color(0xFFF1F5F9) // Dorado o slate 100
                        val chipText = if (isSelected) Color.White else Color(0xFF475569)
                        val chipBorder = if (isSelected) Color.Transparent else Color(0xFFE2E8F0)

                        Box(
                            modifier = Modifier
                                .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(chipBg)
                                .clickable { selectedFilterCommittee = filterName }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filterName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = chipText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showManageCommitteesDialog = true },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF1E3A8A), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Gestionar Comités",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Filter the schedule list
            val currentSelectedMonthName = months[selectedMonthIndex]
            val filteredList = scheduleList.filter { item ->
                val matchesMonth = getMonthOfItem(item, currentSelectedMonthName).equals(currentSelectedMonthName, ignoreCase = true)
                val matchesSearchText = searchQuery.isBlank() ||
                        item.committee.contains(searchQuery, ignoreCase = true) ||
                        item.description.contains(searchQuery, ignoreCase = true) ||
                        item.date.contains(searchQuery, ignoreCase = true)
                val matchesCommitteeChip = selectedFilterCommittee == "Todos" ||
                        item.committee.contains(selectedFilterCommittee, ignoreCase = true)

                matchesMonth && matchesSearchText && matchesCommitteeChip
            }

            // 2. Lista de Eventos por Mes (Tarjetas elegantes con fondo blanco y sombra suave)
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No hay eventos registrados en $currentSelectedMonthName.",
                            color = Color.Gray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (selectedFilterCommittee != "Todos" || searchQuery.isNotBlank()) {
                            Text(
                                text = "Intenta cambiar los filtros de búsqueda.",
                                color = Color.Gray.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        } else {
                            Text(
                                text = "Toca el botón dorado + para agregar un evento.",
                                color = Color.Gray.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        // Fecha exacta o rango de días
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color(0xFFCA8A04) // Dorado
                                            )
                                            Text(
                                                text = item.date,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF1E3A8A) // Azul Rey
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Nombre del Comité o Evento
                                        Text(
                                            text = item.committee,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B) // Dark Slate
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Descripción / Observaciones
                                        Text(
                                            text = item.description,
                                            fontSize = 14.sp,
                                            color = Color(0xFF64748B) // Slate 500
                                        )
                                    }

                                    // Badge / Etiqueta Coloreada según tipo
                                    val badgeText = when {
                                        item.description.contains("ayuno", ignoreCase = true) || item.committee.contains("ayuno", ignoreCase = true) -> "Ayuno"
                                        item.description.contains("especial", ignoreCase = true) || item.committee.contains("especial", ignoreCase = true) -> "Especial"
                                        item.description.contains("reunión", ignoreCase = true) || item.committee.contains("reunion", ignoreCase = true) -> "Reunión"
                                        item.description.contains("vigilia", ignoreCase = true) || item.committee.contains("vigilia", ignoreCase = true) -> "Vigilia"
                                        item.description.contains("culto", ignoreCase = true) || item.committee.contains("culto", ignoreCase = true) -> "Culto"
                                        else -> "Actividad"
                                    }
                                    val badgeColor = when (badgeText) {
                                        "Ayuno" -> Color(0xFF8B5CF6) // Morado
                                        "Especial" -> Color(0xFFD97706) // Oro/Dorado
                                        "Reunión" -> Color(0xFF0D9488) // Teal
                                        "Vigilia" -> Color(0xFF4F46E5) // Indigo
                                        "Culto" -> Color(0xFF1E3A8A) // Azul Rey
                                        else -> Color(0xFF3B82F6) // Azul claro
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(badgeColor.copy(alpha = 0.15f))
                                            .border(1.dp, badgeColor, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = badgeColor
                                        )
                                    }
                                }

                                Divider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color(0xFFF1F5F9)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            editingItem = item
                                            monthState = getMonthOfItem(item, currentSelectedMonthName)
                                            dateState = item.date
                                            committeeState = item.committee
                                            descriptionState = item.description
                                            showDialog = true
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1E3A8A))
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Editar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    TextButton(
                                        onClick = { viewModel.deleteScheduleItem(item.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Eliminar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var monthDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = if (editingItem == null) "Agregar Evento al Cronograma" else "Editar Evento",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Selector de Mes (ExposedDropdownMenuBox)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = monthDropdownExpanded,
                            onExpandedChange = { monthDropdownExpanded = !monthDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = monthState,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mes") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = monthDropdownExpanded,
                                onDismissRequest = { monthDropdownExpanded = false }
                            ) {
                                months.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            monthState = m
                                            monthDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Fecha exacta o rango de días
                    OutlinedTextField(
                        value = dateState,
                        onValueChange = { dateState = it },
                        label = { Text("Fecha o Rango de Días") },
                        placeholder = { Text("Ej: 15 de Agosto / Semana del 10 al 16") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Comité a cargo
                    OutlinedTextField(
                        value = committeeState,
                        onValueChange = { committeeState = it },
                        label = { Text("Comité / Grupo") },
                        placeholder = { Text("Ej: Jóvenes / Damas / Escuela Dominical") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Observaciones
                    OutlinedTextField(
                        value = descriptionState,
                        onValueChange = { descriptionState = it },
                        label = { Text("Observaciones / Actividad") },
                        placeholder = { Text("Ej: Culto Especial de Jóvenes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dateState.isNotBlank() && committeeState.isNotBlank()) {
                            if (editingItem == null) {
                                viewModel.addScheduleItem(dateState, committeeState, descriptionState, monthState)
                            } else {
                                viewModel.updateScheduleItem(
                                    editingItem!!.id,
                                    dateState,
                                    committeeState,
                                    descriptionState,
                                    monthState
                                )
                            }
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showManageCommitteesDialog) {
        var newCommitteeName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showManageCommitteesDialog = false },
            title = {
                Text(
                    text = "Gestionar Comités",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Agrega o elimina los comités de la lista de filtros del cronograma.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommitteeName,
                            onValueChange = { newCommitteeName = it },
                            placeholder = { Text("Nombre del comité") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1E3A8A),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        
                        Button(
                            onClick = {
                                if (newCommitteeName.isNotBlank()) {
                                    viewModel.addCommittee(newCommitteeName)
                                    newCommitteeName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCA8A04)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Añadir", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "Comités Existentes:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (committees.isEmpty()) {
                                item {
                                    Text(
                                        text = "No hay comités personalizados cargados.",
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            } else {
                                items(committees) { comm ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = comm,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF334155)
                                        )
                                        
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteCommittee(comm)
                                                if (selectedFilterCommittee == comm) {
                                                    selectedFilterCommittee = "Todos"
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = Color.Red.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageCommitteesDialog = false }) {
                    Text("Cerrar", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// PANTALLA: MODO PÚLPITO / EXPOSICIÓN (LECTURA)
// -------------------------------------------------------------
@Composable
fun PulpitoScreen(viewModel: WorshipViewModel) {
    val bulletin by viewModel.bulletin.collectAsState()
    val scheduleList by viewModel.scheduleList.collectAsState()
    val context = LocalContext.current

    // Mantener la pantalla encendida usando DisposableEffect nativo de Android
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var textScale by remember { mutableStateOf(16f) } // El tamaño base por defecto ajustado para la tabla
    val royalBlueDark = Color(0xFF1E3A8A)
    val goldAccent = Color(0xFFCA8A04)

    var activeTab by remember { mutableStateOf(0) } // 0: Boletín Semanal, 1: Cronograma Anual

    val months = remember {
        listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
    }
    val currentMonthIndex = remember { java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) }

    val upcomingEvents = remember(scheduleList, currentMonthIndex) {
        fun getMonthIndexOfItem(item: AnnualScheduleItem): Int {
            val monthName = if (item.month.isNotBlank()) {
                item.month
            } else {
                months.firstOrNull { item.date.contains(it, ignoreCase = true) } ?: ""
            }
            val index = months.indexOfFirst { it.equals(monthName, ignoreCase = true) }
            return if (index == -1) currentMonthIndex else index
        }

        scheduleList.filter { item ->
            getMonthIndexOfItem(item) >= currentMonthIndex
        }.sortedWith(compareBy({ getMonthIndexOfItem(it) }, { it.date }))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Fondo oscuro elegante (Navy/Slate)
            .padding(16.dp)
    ) {
        // Cabecera superior del modo púlpito con controles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Boletin) }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Salir",
                    tint = Color.White
                )
            }

            Text(
                text = "MODO PÚLIPTO",
                color = goldAccent, // Oro / Gold para alto contraste
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            // Controles de Tamaño de Texto
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { textScale = (textScale - 1f).coerceAtLeast(11f) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Menos zoom", tint = Color.White)
                }
                Text(
                    text = "${textScale.toInt()}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { textScale = (textScale + 1f).coerceAtMost(28f) }) {
                    Icon(Icons.Default.Add, contentDescription = "Más zoom", tint = Color.White)
                }
            }
        }

        // Pestañas de Navegación en el Modo Púlpito (Boletín Semanal / Cronograma Anual)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "🗓️ Boletín Semanal" to 0,
                "📅 Cronograma Anual" to 1
            ).forEach { (title, index) ->
                val isSelected = activeTab == index
                val tabBg = if (isSelected) Color(0xFF1E3A8A) else Color.Transparent
                val tabBorderColor = if (isSelected) goldAccent else Color.Transparent
                val tabTextColor = if (isSelected) Color.White else Color.LightGray

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tabBg)
                        .then(
                            if (isSelected) Modifier.border(1.dp, tabBorderColor, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                        .clickable { activeTab = index }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = tabTextColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (activeTab == 0) {
            // -------------------------------------------------------------
            // PESTAÑA 0: BOLETÍN SEMANAL
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "BOLETÍN SEMANAL",
                    color = goldAccent,
                    fontSize = (textScale + 6f).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                if (bulletin.dateRange.isNotBlank()) {
                    Text(
                        text = "Semana: ${bulletin.dateRange}",
                        color = Color.White,
                        fontSize = (textScale + 2f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (bulletin.committee.isNotBlank()) {
                    Text(
                        text = "Comité a Cargo: ${bulletin.committee}",
                        color = Color.LightGray,
                        fontSize = textScale.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contenedor principal con Scroll Vertical para la tabla y los anuncios inferiores
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 2. Tabla Completa Semanal
                item {
                    val horizontalScrollState = rememberScrollState()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState)
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp)) // Dark slate card background
                            .border(1.5.dp, goldAccent.copy(alpha = 0.8f), RoundedCornerShape(12.dp)) // bordes dorados
                            .padding(8.dp)
                    ) {
                        Column {
                            // Fila de Encabezados de la Tabla
                            Row(
                                modifier = Modifier
                                    .background(royalBlueDark) // Encabezado en Azul Rey elegante
                                    .padding(vertical = 8.dp)
                            ) {
                                TableHeaderCell(text = "Día", width = 90.dp, size = textScale)
                                TableHeaderCell(text = "Culto / Servicio", width = 160.dp, size = textScale)
                                TableHeaderCell(text = "Hora", width = 80.dp, size = textScale)
                                TableHeaderCell(text = "Ujieres", width = 140.dp, size = textScale)
                                TableHeaderCell(text = "Alabanza", width = 140.dp, size = textScale)
                                TableHeaderCell(text = "Decom", width = 110.dp, size = textScale)
                                TableHeaderCell(text = "Sonido", width = 110.dp, size = textScale)
                            }

                            HorizontalDivider(color = goldAccent, thickness = 1.5.dp)

                            // Filas para los 7 días de la semana
                            bulletin.days.forEach { day ->
                                Row(
                                    modifier = Modifier
                                        .padding(vertical = 6.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TableCell(text = day.dayName, width = 90.dp, size = textScale, isDayName = true)
                                    TableCell(text = day.serviceName, width = 160.dp, size = textScale)
                                    TableCell(text = day.time, width = 80.dp, size = textScale)
                                    TableCell(text = day.ushers, width = 140.dp, size = textScale)
                                    TableCell(text = day.worshipTeam, width = 140.dp, size = textScale)
                                    TableCell(text = day.decom, width = 110.dp, size = textScale)
                                    TableCell(text = day.sound, width = 110.dp, size = textScale)
                                }
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 0.5.dp)
                            }
                        }
                    }
                }

                // Indicador de deslizamiento horizontal si es necesario
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = goldAccent.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Desliza lateralmente para ver la tabla completa",
                            color = Color.LightGray.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 3. Bloque Inferior: Anuncios Generales
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .border(1.5.dp, goldAccent.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "ANUNCIOS GENERALES Y AVISOS",
                                color = goldAccent,
                                fontSize = (textScale + 2f).sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        HorizontalDivider(color = goldAccent.copy(alpha = 0.4f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = bulletin.generalAnnouncements.ifBlank { "No hay anuncios generales registrados para esta semana." },
                            color = Color.White,
                            fontSize = textScale.sp,
                            lineHeight = (textScale * 1.4f).sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        } else {
            // -------------------------------------------------------------
            // PESTAÑA 1: CRONOGRAMA ANUAL / PRÓXIMOS EVENTOS
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "CRONOGRAMA ANUAL",
                    color = goldAccent,
                    fontSize = (textScale + 6f).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Próximos Eventos del Año (${months[currentMonthIndex]} en adelante)",
                    color = Color.White,
                    fontSize = (textScale).sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (upcomingEvents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No hay eventos programados para el resto del año.",
                                    color = Color.LightGray,
                                    fontSize = textScale.coerceAtLeast(14f).sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(upcomingEvents) { item ->
                        val badgeText = when {
                            item.description.contains("ayuno", ignoreCase = true) || item.committee.contains("ayuno", ignoreCase = true) -> "Ayuno"
                            item.description.contains("especial", ignoreCase = true) || item.committee.contains("especial", ignoreCase = true) -> "Especial"
                            item.description.contains("reunión", ignoreCase = true) || item.committee.contains("reunion", ignoreCase = true) -> "Reunión"
                            item.description.contains("vigilia", ignoreCase = true) || item.committee.contains("vigilia", ignoreCase = true) -> "Vigilia"
                            item.description.contains("culto", ignoreCase = true) || item.committee.contains("culto", ignoreCase = true) -> "Culto"
                            else -> "Actividad"
                        }
                        val badgeColor = when (badgeText) {
                            "Ayuno" -> Color(0xFFC084FC) // Morado claro para fondo oscuro
                            "Especial" -> Color(0xFFFBBF24) // Dorado/Amarillo
                            "Reunión" -> Color(0xFF2DD4BF) // Teal
                            "Vigilia" -> Color(0xFF818CF8) // Indigo
                            "Culto" -> Color(0xFF60A5FA) // Azul
                            else -> Color(0xFF94A3B8) // Slate gray
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .border(1.5.dp, goldAccent.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            // Rango de Días y Mes
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = goldAccent,
                                    modifier = Modifier.size((textScale + 2f).coerceAtLeast(18f).dp)
                                )
                                val resolvedMonthName = if (item.month.isNotBlank()) item.month else {
                                    months.firstOrNull { item.date.contains(it, ignoreCase = true) } ?: months[currentMonthIndex]
                                }
                                Text(
                                    text = "${resolvedMonthName.uppercase()} - ${item.date}",
                                    color = goldAccent,
                                    fontSize = (textScale + 2f).coerceAtLeast(18f).sp, // Mantiene tipografía grande de 18px+
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Comité o Evento + Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeColor.copy(alpha = 0.2f))
                                        .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = (textScale - 4f).coerceAtLeast(11f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }

                                Text(
                                    text = item.committee,
                                    color = Color.White,
                                    fontSize = (textScale + 1f).coerceAtLeast(16f).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Notas / Detalles / Observaciones
                            if (item.description.isNotBlank()) {
                                Text(
                                    text = item.description,
                                    color = Color.LightGray,
                                    fontSize = textScale.coerceAtLeast(14f).sp,
                                    lineHeight = (textScale * 1.35f).coerceAtLeast(18f).sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp, size: Float) {
    Text(
        text = text,
        color = Color(0xFFFEF3C7), // Light Gold text
        fontSize = (size - 1f).coerceAtLeast(11f).sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp),
        textAlign = TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun TableCell(text: String, width: androidx.compose.ui.unit.Dp, size: Float, isDayName: Boolean = false) {
    val displayStr = if (text.isBlank()) "-" else text
    Text(
        text = displayStr,
        color = if (isDayName) Color(0xFFCA8A04) else if (text.isBlank()) Color.Gray else Color.White,
        fontSize = (size - 2f).coerceAtLeast(10f).sp,
        fontWeight = if (isDayName) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp),
        textAlign = TextAlign.Start,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}


// -------------------------------------------------------------
// MÓDULO EXPORTACIÓN: PDF Y COMPARTIR WHATSAPP
// -------------------------------------------------------------
fun printPdf(context: Context, bulletin: WeeklyBulletin) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "Boletín Eclesiástico - ${bulletin.dateRange}"

    printManager.print(jobName, object : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }

            val info = PrintDocumentInfo.Builder("boletin_eclesiastico.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()

            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out android.print.PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val pdfDocument = PrintedPdfDocument(
                context,
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("id", "print", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
            )

            val page = pdfDocument.startPage(1)
            val canvas = page.canvas

            val headerBgPaint = Paint().apply {
                color = AndroidColor.parseColor("#EFF6FF") // Light Royal Blue background
                style = Paint.Style.FILL
            }

            val tableHeaderPaint = Paint().apply {
                color = AndroidColor.parseColor("#1E3A8A") // Royal Blue
                style = Paint.Style.FILL
            }

            val titlePaint = Paint().apply {
                color = AndroidColor.parseColor("#1E3A8A") // Royal Blue
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = AndroidColor.parseColor("#1E293B") // Slate text
                textSize = 11f
                isAntiAlias = true
            }

            val cellPaint = Paint().apply {
                color = AndroidColor.parseColor("#1E293B") // Slate text
                textSize = 9f
                isAntiAlias = true
            }

            val boldCellPaint = Paint().apply {
                color = AndroidColor.parseColor("#CA8A04") // Gold Accent for day names
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val headerTextPaint = Paint().apply {
                color = AndroidColor.WHITE
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val borderPaint = Paint().apply {
                color = AndroidColor.parseColor("#E2E8F0") // Slate Light Border
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val royalBorderPaint = Paint().apply {
                color = AndroidColor.parseColor("#1E3A8A") // Royal Blue border
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val goldBorderPaint = Paint().apply {
                color = AndroidColor.parseColor("#CA8A04") // Gold border
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val width = canvas.width
            val height = canvas.height

            // Encabezado del PDF
            canvas.drawRect(30f, 30f, width - 30f, 90f, headerBgPaint)
            canvas.drawRect(30f, 30f, width - 30f, 90f, royalBorderPaint)

            canvas.drawText("SECRETARÍA ECLESIÁSTICA: BOLETÍN SEMANAL", 45f, 55f, titlePaint)
            canvas.drawText(
                "Rango de Fechas: ${bulletin.dateRange}   |   Comité a Cargo: ${bulletin.committee}",
                45f,
                80f,
                subtitlePaint
            )

            // Dibujar la Tabla de Servidores Diarios
            var y = 110f
            val rowHeight = 25f
            val headers = arrayOf("Día", "Servicio", "Hora", "Alabanza", "Ujieres", "DECOM/Sonido", "Uniforme")

            // Encabezado de la Tabla
            canvas.drawRect(30f, y, width - 30f, y + rowHeight, tableHeaderPaint)
            canvas.drawRect(30f, y, width - 30f, y + rowHeight, royalBorderPaint)

            var x = 30f
            val colSize = (width - 60f) / headers.size
            for (i in headers.indices) {
                canvas.drawText(headers[i], x + 5f, y + 16f, headerTextPaint)
                x += colSize
            }
            y += rowHeight

            // Filas de la Tabla (Lunes a Domingo)
            for (day in bulletin.days) {
                canvas.drawRect(30f, y, width - 30f, y + rowHeight, borderPaint)

                val cells = arrayOf(
                    day.dayName,
                    day.serviceName,
                    day.time,
                    day.worshipTeam,
                    day.ushers,
                    "${day.decom}/${day.sound}",
                    day.notesUniform
                )

                x = 30f
                for (i in cells.indices) {
                    val text = cells[i]
                    val drawText = if (text.length > 15) text.take(13) + ".." else text
                    val paintToUse = if (i == 0) boldCellPaint else cellPaint
                    canvas.drawText(drawText, x + 5f, y + 16f, paintToUse)
                    x += colSize
                }
                y += rowHeight
            }

            // Cuadro de Anuncios Generales
            y += 20f
            canvas.drawRect(30f, y, width - 30f, y + 150f, borderPaint)
            canvas.drawRect(30f, y, width - 30f, y + 25f, headerBgPaint)
            canvas.drawRect(30f, y, width - 30f, y + 25f, royalBorderPaint)

            val annTitlePaint = Paint().apply {
                color = AndroidColor.parseColor("#1E3A8A")
                textSize = 10f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("ANUNCIOS GENERALES Y AVISOS DE LA SEMANA", 40f, y + 17f, annTitlePaint)
            y += 35f

            val lines = bulletin.generalAnnouncements.split("\n")
            var lineY = y
            for (line in lines) {
                if (lineY < height - 40f) {
                    val wrapText = if (line.length > 90) line.take(87) + "..." else line
                    canvas.drawText(wrapText, 45f, lineY, cellPaint)
                    lineY += 14f
                }
            }

            pdfDocument.finishPage(page)

            try {
                pdfDocument.writeTo(java.io.FileOutputStream(destination.fileDescriptor))
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                pdfDocument.close()
            }
        }
    }, null)
}

fun shareOnWhatsApp(context: Context, bulletin: WeeklyBulletin) {
    val formattedText = buildString {
        append("📋 *BOLETÍN SEMANAL Y ANUNCIOS* 📋\n")
        append("📅 _Rango de Fechas:_ ${bulletin.dateRange}\n")
        append("👥 _Comité / Grupo a Cargo:_ ${bulletin.committee}\n\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("✨ *CRONOGRAMA DE SERVICIOS* ✨\n")
        append("━━━━━━━━━━━━━━━━━━━━\n\n")

        for (day in bulletin.days) {
            if (day.serviceName.isNotBlank()) {
                append("📆 *${day.dayName.uppercase()}*\n")
                append("⛪ _Culto:_ ${day.serviceName}\n")
                if (day.time.isNotBlank()) append("⏰ _Hora:_ ${day.time}\n")
                if (day.worshipTeam.isNotBlank()) append("🎵 _Alabanza:_ ${day.worshipTeam}\n")
                if (day.ushers.isNotBlank()) append("🤝 _Ujieres:_ ${day.ushers}\n")
                if (day.decom.isNotBlank() || day.sound.isNotBlank()) {
                    append("💻 _Multimedia/Sonido:_ ${day.decom} / ${day.sound}\n")
                }
                if (day.notesUniform.isNotBlank()) append("👔 _Uniforme/Notas:_ ${day.notesUniform}\n")
                append("\n")
            }
        }

        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("📢 *ANUNCIOS Y AVISOS* 📢\n")
        append("━━━━━━━━━━━━━━━━━━━━\n\n")
        if (bulletin.generalAnnouncements.isNotBlank()) {
            append(bulletin.generalAnnouncements)
        } else {
            append("No hay anuncios registrados para esta semana.")
        }
    }

    // Copiar al Portapapeles
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Boletin Eclesiastico", formattedText)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Resumen copiado al portapapeles", Toast.LENGTH_SHORT).show()

    // Compartir a WhatsApp o General
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, formattedText)
        type = "text/plain"
    }

    try {
        val shareIntent = Intent.createChooser(sendIntent, "Compartir Boletín")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
