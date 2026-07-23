package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.Mosaic
import com.example.data.model.Song
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.WorshipViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val CategoryList = listOf(
    "Coros", "Himnos", "Himnos Especiales", "Coros Infantiles",
    "Alabanzas", "Adoración", "Santa Cena", "Evangelismo", "Otros"
)

fun parseChordLine(line: String): Pair<String, String>? {
    if (!line.contains("[")) return null
    val chordBuilder = StringBuilder()
    val lyricBuilder = StringBuilder()
    var i = 0
    while (i < line.length) {
        if (line[i] == '[') {
            val endBracket = line.indexOf(']', i)
            if (endBracket != -1) {
                val chord = line.substring(i + 1, endBracket)
                val lyricLen = lyricBuilder.length
                while (chordBuilder.length < lyricLen) {
                    chordBuilder.append(' ')
                }
                chordBuilder.append(chord)
                i = endBracket + 1
                continue
            }
        }
        lyricBuilder.append(line[i])
        i++
    }
    // Ensure both builders have the exact same length by padding with spaces
    val maxLen = maxOf(chordBuilder.length, lyricBuilder.length)
    while (chordBuilder.length < maxLen) {
        chordBuilder.append(' ')
    }
    while (lyricBuilder.length < maxLen) {
        lyricBuilder.append(' ')
    }
    return Pair(chordBuilder.toString(), lyricBuilder.toString())
}

@Composable
fun AppNavigationWrapper(viewModel: WorshipViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeOverlayMessage by viewModel.activeOverlayMessage.collectAsState()

    // Handle Android system back presses safely
    BackHandler(enabled = currentScreen != Screen.Home) {
        viewModel.navigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Screen Router
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is Screen.Home -> HomeScreen(viewModel)
                is Screen.SongBook -> SongBookScreen(viewModel)
                is Screen.SongDetail -> SongDetailScreen(viewModel, screen.songId)
                is Screen.EditSong -> EditSongScreen(viewModel, screen.songId)
                is Screen.WorshipMode -> WorshipModeScreen(viewModel, screen.songId, screen.mosaicId, screen.mosaicSongIndex)
                is Screen.Mosaics -> MosaicsScreen(viewModel)
                is Screen.CreateMosaic -> CreateMosaicScreen(viewModel)
                is Screen.TodayList -> TodayListScreen(viewModel)
                is Screen.TodayListHistoryScreen -> TodayListHistoryScreen(viewModel)
                is Screen.TodayListHistoryDetail -> TodayListHistoryDetailScreen(viewModel, screen.historyId)
                is Screen.Favorites -> FavoritesScreen(viewModel)
                is Screen.Search -> SearchScreen(viewModel)
                is Screen.SessionLeader -> SessionLeaderScreen(viewModel)
                is Screen.SessionMember -> SessionMemberScreen(viewModel)
                is Screen.ImportExport -> ImportExportScreen(viewModel)
                is Screen.Settings -> SettingsScreen(viewModel)
            }
        }

        // Leader message overlay for team members
        activeOverlayMessage?.let { msg ->
            val isWorshipMode = currentScreen is Screen.WorshipMode
            if (isWorshipMode) {
                // Top banner notification (large, elegant, temporary, non-blocking)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val backgroundColor = when {
                        msg.contains("REPETIMOS") || msg.contains("Repetimos") || msg.uppercase().contains("CORO") -> Color(0xFF1565C0) // Rich Blue
                        msg.contains("SOLO") || msg.contains("Solo") || msg.uppercase().contains("PIANO") || msg.uppercase().contains("GUITARRA") -> Color(0xFF2E7D32) // Rich Green
                        msg.contains("TODA") || msg.contains("Toda") || msg.uppercase().contains("BANDA") -> Color(0xFFEF6C00) // Rich Orange
                        msg.contains("TERMINAMOS") || msg.contains("Finalizar") || msg.contains("Terminar") || msg.uppercase().contains("FIN") -> Color(0xFFC62828) // Rich Red
                        msg.contains("abrió") || msg.contains("abrio") || msg.contains("Director") -> Color(0xFF673AB7) // Beautiful Purple
                        else -> Color(0xFF37474F) // Slate Grey
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leader_message_top_overlay")
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (msg.contains("abrió") || msg.contains("abrio") || msg.contains("Director")) Icons.Default.QueueMusic else Icons.Default.Campaign,
                                    contentDescription = "Mensaje del Director",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (msg.contains("abrió") || msg.contains("abrio") || msg.contains("Director")) "NUEVA CANCIÓN" else "INSTRUCCIÓN DEL DIRECTOR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = msg,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Centered overlay (original behavior for other screens)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable(enabled = false) {}, // Scrim block
                    contentAlignment = Alignment.Center
                ) {
                    val backgroundColor = when {
                        msg.contains("REPETIMOS") || msg.contains("Repetimos") -> Color(0xFF0F4C81) // Deep Blue
                        msg.contains("SOLO") || msg.contains("Solo") -> Color(0xFF2D6A4F) // Green
                        msg.contains("TODA") || msg.contains("Toda") -> Color(0xFFD68A1A) // Warm Yellow/Orange
                        msg.contains("TERMINAMOS") || msg.contains("Finalizar") || msg.contains("Terminar") -> Color(0xFF9E2A2B) // Red
                        else -> Color(0xFF2D3142) // Dark Slate
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp)
                            .testTag("leader_message_overlay")
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(28.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Mensaje del Director",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = msg,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 40.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// 1. HOME SCREEN
@Composable
fun HomeScreen(viewModel: WorshipViewModel) {
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val memberStatus by viewModel.memberStatus.collectAsState()
    val favoritesList by viewModel.favorites.collectAsState()
    val todaySongIds by viewModel.todaySongIds.collectAsState()
    val favoritesCount = favoritesList.size
    val favoritesSubtitle = if (favoritesCount == 0) {
        "Mis canciones preferidas"
    } else if (favoritesCount == 1) {
        "1 canción guardada"
    } else {
        "$favoritesCount canciones guardadas"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_worship_banner),
                contentDescription = "Cancionero Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )
            // Header Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Cancionero de Alabanza",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ministerio de Música y Adoración",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        // Live Mode Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSessionActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF2E7D32).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Director Activo",
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (memberStatus == com.example.network.WorshipClient.ConnectionStatus.CONNECTED) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF1565C0).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF2196F3))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Conectado al Director",
                        color = Color(0xFF2196F3),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Action Grid List
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            item(span = { GridItemSpan(2) }) {
                val todaySubtitle = if (todaySongIds.isEmpty()) "Lista para el culto o ensayo de hoy" else "${todaySongIds.size} canción${if (todaySongIds.size == 1) "" else "es"} en la lista de hoy"
                FeaturedHomeCard(
                    title = "Lista del Día",
                    subtitle = todaySubtitle,
                    icon = Icons.Default.Today,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    iconContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    iconColor = MaterialTheme.colorScheme.tertiaryContainer,
                    tag = "btn_lista_del_dia"
                ) {
                    viewModel.navigateTo(Screen.TodayList)
                }
            }
            item(span = { GridItemSpan(1) }) {
                HomeCard(
                    title = "Cancionero",
                    subtitle = "Todas las canciones",
                    icon = Icons.Default.LibraryMusic,
                    color = Color(0xFFD0BCFF),
                    tag = "btn_cancionero"
                ) {
                    viewModel.navigateTo(Screen.SongBook)
                }
            }
            item(span = { GridItemSpan(1) }) {
                HomeCard(
                    title = "Mosaicos",
                    subtitle = "Listas y setlists",
                    icon = Icons.Default.QueueMusic,
                    color = Color(0xFF009688),
                    tag = "btn_mosaicos"
                ) {
                    viewModel.navigateTo(Screen.Mosaics)
                }
            }
            item(span = { GridItemSpan(1) }) {
                HomeCard(
                    title = "Favoritos",
                    subtitle = favoritesSubtitle,
                    icon = Icons.Default.Favorite,
                    color = Color(0xFFE91E63),
                    tag = "btn_favoritos"
                ) {
                    viewModel.navigateTo(Screen.Favorites)
                }
            }
            item(span = { GridItemSpan(1) }) {
                HomeCard(
                    title = "Buscar",
                    subtitle = "Búsqueda instantánea",
                    icon = Icons.Default.Search,
                    color = Color(0xFF2196F3),
                    tag = "btn_buscar"
                ) {
                    viewModel.navigateTo(Screen.Search)
                }
            }
            item(span = { GridItemSpan(2) }) {
                FeaturedHomeCard(
                    title = "Modo Director",
                    subtitle = "Controlar sesión en vivo",
                    icon = Icons.Default.Campaign,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    iconContainerColor = MaterialTheme.colorScheme.onPrimary,
                    iconColor = MaterialTheme.colorScheme.primary,
                    tag = "btn_director"
                ) {
                    viewModel.navigateTo(Screen.SessionLeader)
                }
            }
            item(span = { GridItemSpan(2) }) {
                FeaturedHomeCard(
                    title = "Modo Integrante",
                    subtitle = "Unirse a la sesión activa",
                    icon = Icons.Default.Group,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconColor = MaterialTheme.colorScheme.secondaryContainer,
                    tag = "btn_integrante"
                ) {
                    viewModel.navigateTo(Screen.SessionMember)
                }
            }
            item(span = { GridItemSpan(1) }) {
                HomeCard(
                    title = "Importar/Exportar",
                    subtitle = "Compartir canciones",
                    icon = Icons.Default.SwapHoriz,
                    color = Color(0xFF4CAF50),
                    tag = "btn_import_export"
                ) {
                    viewModel.navigateTo(Screen.ImportExport)
                }
            }
            item(span = { GridItemSpan(1) }) {
                HomeCard(
                    title = "Configuración",
                    subtitle = "Ajustar preferencias",
                    icon = Icons.Default.Settings,
                    color = Color(0xFF607D8B),
                    tag = "btn_configuracion"
                ) {
                    viewModel.navigateTo(Screen.Settings)
                }
            }
        }
    }
}

@Composable
fun HomeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
            .testTag(tag),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FeaturedHomeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    iconContainerColor: Color,
    iconColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() }
            .testTag(tag),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


// 2. SONGBOOK SCREEN
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongBookScreen(viewModel: WorshipViewModel) {
    val songList by viewModel.songs.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedCategory by remember { mutableStateOf(viewModel.songBookSelectedCategory) }
    var selectedKey by remember { mutableStateOf(viewModel.songBookSelectedKey) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateListOf<Long>() }

    var showBatchCategoryDialog by remember { mutableStateOf(false) }
    var showBatchKeyDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchMosaicDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.songBookSelectedCategory, viewModel.songBookSelectedKey) {
        selectedCategory = viewModel.songBookSelectedCategory
        selectedKey = viewModel.songBookSelectedKey
    }

    LaunchedEffect(selectedCategory, selectedKey) {
        viewModel.songBookSelectedCategory = selectedCategory
        viewModel.songBookSelectedKey = selectedKey
    }

    val categoriesWithAll = remember(categories) { listOf("Todas", "Favoritos") + categories }
    val sortMode = viewModel.songBookSortMode
    val filteredSongs = remember(songList, selectedCategory, selectedKey, sortMode) {
        val baseFiltered = songList.filter { song ->
            val matchesCategory = when (selectedCategory) {
                "Todas" -> true
                "Favoritos" -> song.isFavorite
                else -> song.category == selectedCategory
            }
            val matchesKey = selectedKey == null || song.key.equals(selectedKey, ignoreCase = true)
            matchesCategory && matchesKey
        }
        when (sortMode) {
            "Título" -> baseFiltered.sortedBy { it.title.lowercase() }
            "Tono" -> baseFiltered.sortedBy { it.key }
            "Categoría" -> baseFiltered.sortedBy { it.category }
            else -> baseFiltered.sortedBy { it.displayOrder }
        }
    }

    val listState = rememberLazyListState()
    var isScrollRestored by remember { mutableStateOf(false) }

    LaunchedEffect(filteredSongs) {
        if (filteredSongs.isNotEmpty() && !isScrollRestored) {
            if (viewModel.songBookScrollIndex < filteredSongs.size) {
                listState.scrollToItem(viewModel.songBookScrollIndex, viewModel.songBookScrollOffset)
            }
            isScrollRestored = true
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            .collect { (index, offset) ->
                if (isScrollRestored) {
                    viewModel.songBookScrollIndex = index
                    viewModel.songBookScrollOffset = offset
                }
            }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedSongIds.clear()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancelar selección")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedSongIds.isEmpty()) "Seleccionar canciones" else "${selectedSongIds.size} seleccionadas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            if (selectedSongIds.size == filteredSongs.size) {
                                selectedSongIds.clear()
                            } else {
                                selectedSongIds.clear()
                                selectedSongIds.addAll(filteredSongs.map { it.id })
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selectedSongIds.size == filteredSongs.size && filteredSongs.isNotEmpty()) Icons.Default.DoneAll else Icons.Default.SelectAll,
                            contentDescription = "Seleccionar todas"
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigateBack() },
                            modifier = Modifier.testTag("btn_back")
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cancionero",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { isSelectionMode = true },
                        modifier = Modifier.testTag("btn_multi_select")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Seleccionar varias")
                    }
                }
            }
        },
        bottomBar = {
            if (isSelectionMode && selectedSongIds.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Asignar Categoría
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showBatchCategoryDialog = true }
                                .padding(8.dp)
                                .testTag("btn_batch_category")
                        ) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Categoría", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Asignar Tono
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showBatchKeyDialog = true }
                                .padding(8.dp)
                                .testTag("btn_batch_key")
                        ) {
                            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Tono / Nota", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Crear Mosaico
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showBatchMosaicDialog = true }
                                .padding(8.dp)
                                .testTag("btn_batch_mosaic")
                        ) {
                            Icon(imageVector = Icons.Default.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Mosaico", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Eliminar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showBatchDeleteDialog = true }
                                .padding(8.dp)
                                .testTag("btn_batch_delete")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Eliminar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(Screen.EditSong()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_song")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar Canción")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal scrollable categories
            ScrollableTabRow(
                selectedTabIndex = categoriesWithAll.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 16.dp,
                divider = {}
            ) {
                categoriesWithAll.forEachIndexed { _, cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (cat == "Favoritos") {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = if (selectedCategory == cat) Color.Red else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(cat, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            // Sort Selector Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Orden:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sortOptions = listOf(
                        "Manual" to "Manual",
                        "Título" to "Título",
                        "Tono" to "Tono",
                        "Categoría" to "Cat."
                    )
                    sortOptions.forEach { (mode, label) ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = { viewModel.songBookSortMode = mode },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // Key Filter Active Tag Bar if key filter is set
            if (selectedKey != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Filtrando nota:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        FilterChip(
                            selected = true,
                            onClick = {
                                selectedKey = null
                                viewModel.songBookSelectedKey = null
                            },
                            label = { Text("Tono: $selectedKey", fontWeight = FontWeight.Bold) },
                            trailingIcon = {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Quitar filtro de tono", modifier = Modifier.size(14.dp))
                            }
                        )
                    }

                    TextButton(
                        onClick = {
                            selectedKey = null
                            viewModel.songBookSelectedKey = null
                            selectedCategory = "Todas"
                            viewModel.songBookSelectedCategory = "Todas"
                        }
                    ) {
                        Text("Limpiar filtros", fontSize = 12.sp)
                    }
                }
            }

            if (filteredSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedKey != null) "No hay canciones con tono '$selectedKey' en '$selectedCategory'." else "No hay canciones en esta categoría.",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    itemsIndexed(filteredSongs, key = { _, song -> song.id }) { index, song ->
                        val isSelected = selectedSongIds.contains(song.id)
                        SongRowItem(
                            song = song,
                            onClick = {
                                viewModel.navigateTo(Screen.SongDetail(song.id))
                            },
                            onFavoriteToggle = {
                                viewModel.toggleFavorite(song)
                            },
                            onAddToTodayList = {
                                viewModel.addToTodayList(song.id)
                            },
                            onMoveUp = if (sortMode == "Manual" && index > 0) {
                                { viewModel.reorderSongs(index, index - 1, filteredSongs) }
                            } else null,
                            onMoveDown = if (sortMode == "Manual" && index < filteredSongs.size - 1) {
                                { viewModel.reorderSongs(index, index + 1, filteredSongs) }
                            } else null,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onSelectToggle = {
                                if (isSelected) {
                                    selectedSongIds.remove(song.id)
                                } else {
                                    selectedSongIds.add(song.id)
                                    isSelectionMode = true
                                }
                            },
                            onCategoryClick = { cat ->
                                selectedCategory = cat
                                viewModel.songBookSelectedCategory = cat
                            },
                            onKeyClick = { key ->
                                selectedKey = key
                                viewModel.songBookSelectedKey = key
                            }
                        )
                        if (index < filteredSongs.size - 1) {
                            Divider(
                                modifier = Modifier.padding(top = 10.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }

        // BATCH DIALOGS
        if (showBatchCategoryDialog) {
            var newCategoryInput by remember { mutableStateOf("") }
            var selectedDialogCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Coros") }

            AlertDialog(
                onDismissRequest = { showBatchCategoryDialog = false },
                title = { Text("Asignar Categoría Masiva") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Selecciona o escribe la nueva categoría para las ${selectedSongIds.size} canciones seleccionadas:")

                        Text("Categorías existentes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                        ) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedDialogCategory == cat && newCategoryInput.isEmpty(),
                                    onClick = {
                                        selectedDialogCategory = cat
                                        newCategoryInput = ""
                                    },
                                    label = { Text(cat, fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        OutlinedTextField(
                            value = newCategoryInput,
                            onValueChange = { newCategoryInput = it },
                            label = { Text("O escribe una nueva categoría") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val categoryToApply = if (newCategoryInput.trim().isNotEmpty()) newCategoryInput.trim() else selectedDialogCategory
                            if (categoryToApply.isNotEmpty()) {
                                viewModel.updateBatchCategory(selectedSongIds, categoryToApply) {
                                    showBatchCategoryDialog = false
                                    isSelectionMode = false
                                    selectedSongIds.clear()
                                }
                            }
                        }
                    ) {
                        Text("Aplicar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchCategoryDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showBatchKeyDialog) {
            val commonKeys = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B", "Cm", "Dm", "Em", "Fm", "Gm", "Am", "Bm")
            var selectedDialogKey by remember { mutableStateOf("G") }
            var customKeyInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showBatchKeyDialog = false },
                title = { Text("Asignar Tono / Nota Masivo") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Selecciona o ingresa el tono para las ${selectedSongIds.size} canciones seleccionadas:")

                        Text("Tonos comunes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                        ) {
                            items(commonKeys) { key ->
                                FilterChip(
                                    selected = selectedDialogKey == key && customKeyInput.isEmpty(),
                                    onClick = {
                                        selectedDialogKey = key
                                        customKeyInput = ""
                                    },
                                    label = { Text(key, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customKeyInput,
                            onValueChange = { customKeyInput = it },
                            label = { Text("Otro tono o nota personalizada") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val keyToApply = if (customKeyInput.trim().isNotEmpty()) customKeyInput.trim() else selectedDialogKey
                            if (keyToApply.isNotEmpty()) {
                                viewModel.updateBatchKey(selectedSongIds, keyToApply) {
                                    showBatchKeyDialog = false
                                    isSelectionMode = false
                                    selectedSongIds.clear()
                                }
                            }
                        }
                    ) {
                        Text("Aplicar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchKeyDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showBatchMosaicDialog) {
            var mosaicNameInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showBatchMosaicDialog = false },
                title = { Text("Crear Mosaico con Selección") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Se incluirán ${selectedSongIds.size} canciones en este mosaico.")
                        OutlinedTextField(
                            value = mosaicNameInput,
                            onValueChange = { mosaicNameInput = it },
                            label = { Text("Nombre del Mosaico") },
                            placeholder = { Text("Ej: Culto de Domingo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = mosaicNameInput.trim().isNotEmpty(),
                        onClick = {
                            if (mosaicNameInput.trim().isNotEmpty()) {
                                viewModel.saveMosaic(mosaicNameInput.trim(), selectedSongIds) {
                                    showBatchMosaicDialog = false
                                    isSelectionMode = false
                                    selectedSongIds.clear()
                                }
                            }
                        }
                    ) {
                        Text("Crear Mosaico")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchMosaicDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showBatchDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteDialog = false },
                title = { Text("¿Eliminar canciones?") },
                text = {
                    Text("Se eliminarán ${selectedSongIds.size} canciones de forma permanente del cancionero.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteBatchSongs(selectedSongIds) {
                                showBatchDeleteDialog = false
                                isSelectionMode = false
                                selectedSongIds.clear()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRowItem(
    song: Song,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToTodayList: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: (() -> Unit)? = null,
    onCategoryClick: ((String) -> Unit)? = null,
    onKeyClick: ((String) -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectToggle?.invoke()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    onSelectToggle?.invoke()
                }
            )
            .testTag("song_item_${song.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggle?.invoke() },
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = {
                            if (!isSelectionMode && onCategoryClick != null) {
                                onCategoryClick(song.category)
                            } else if (isSelectionMode) {
                                onSelectToggle?.invoke()
                            }
                        },
                        label = { Text(song.category, fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                    SuggestionChip(
                        onClick = {
                            if (!isSelectionMode && onKeyClick != null) {
                                onKeyClick(song.key)
                            } else if (isSelectionMode) {
                                onSelectToggle?.invoke()
                            }
                        },
                        label = { Text("Tono: ${song.key}", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            if (!isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onMoveUp != null || onMoveDown != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((-4).dp)
                        ) {
                            IconButton(
                                onClick = { onMoveUp?.invoke() },
                                enabled = onMoveUp != null,
                                modifier = Modifier.size(32.dp).testTag("move_up_${song.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Subir canción",
                                    tint = if (onMoveUp != null) MaterialTheme.colorScheme.primary else Color.LightGray
                                )
                            }
                            IconButton(
                                onClick = { onMoveDown?.invoke() },
                                enabled = onMoveDown != null,
                                modifier = Modifier.size(32.dp).testTag("move_down_${song.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Bajar canción",
                                    tint = if (onMoveDown != null) MaterialTheme.colorScheme.primary else Color.LightGray
                                )
                            }
                        }
                    }
                    if (onAddToTodayList != null) {
                        IconButton(
                            onClick = onAddToTodayList,
                            modifier = Modifier.testTag("add_to_today_list_${song.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Agregar a Lista del Día",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (song.isFavorite) Color.Red else Color.Gray
                        )
                    }
                }
            }
        }
    }
}


// 3. SONG DETAIL SCREEN
@Composable
fun SongDetailScreen(viewModel: WorshipViewModel, songId: Long) {
    val songList by viewModel.songs.collectAsState()
    val song = remember(songList, songId) { songList.find { it.id == songId } }
    val context = LocalContext.current

    var showChords by remember { mutableStateOf(viewModel.showChordsByDefault) }
    var currentFontSize by remember { mutableStateOf(16f) }

    if (song == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Canción no encontrada")
        }
        return
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detalle de Canción",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (song.isFavorite) Color.Red else Color.Gray
                        )
                    }
                    IconButton(onClick = { viewModel.navigateTo(Screen.EditSong(song.id)) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Header stats
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = song.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(
                            onClick = { viewModel.selectCategoryFilter(song.category) },
                            label = { Text(song.category) }
                        )
                        SuggestionChip(
                            onClick = { viewModel.selectKeyFilter(song.key) },
                            label = { Text("Tono: ${song.key}", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes if any
            if (song.notes.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notas: ${song.notes}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quick Actions Scroll
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(Screen.WorshipMode(song.id)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_pantalla_completa")
                ) {
                    Icon(imageVector = Icons.Default.Fullscreen, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pantalla Completa")
                }

                OutlinedButton(
                    onClick = {
                        val shareText = """
                            *${song.title}* (${song.category}) - Tono: ${song.key}
                            
                            ${song.lyrics.replace(Regex("\\[.*?\\]"), "")}
                            
                            Enviado desde Cancionero de Alabanza
                        """.trimIndent()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Compartir Canción")
                        context.startActivity(shareIntent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartir")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Controls & Header row for the lyrics section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Letra & Acordes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Interactive Chords Toggle & Font Size controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { currentFontSize = (currentFontSize - 2f).coerceAtLeast(12f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Reducir letra",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "${currentFontSize.toInt()}sp",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { currentFontSize = (currentFontSize + 2f).coerceAtMost(28f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aumentar letra",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { showChords = !showChords },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_toggle_chords_detail")
                    ) {
                        Icon(
                            imageVector = if (showChords) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Mostrar/Ocultar Acordes",
                            tint = if (showChords) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lyrics Box - with a beautiful bordered card that scrolls smoothly
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.isDarkMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else Color.White
                ),
                border = BorderStroke(1.dp, if (viewModel.isDarkMode) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f) else Color(0xFFE0E0E0)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (viewModel.isDarkMode) 0.dp else 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val lines = song.lyrics.split("\n")
                    items(lines) { line ->
                        LyricLineView(line = line, showChords = showChords, fontSize = currentFontSize, isDark = viewModel.isDarkMode)
                    }
                }
            }
        }
    }
}


// 4. EDIT / CREATE SONG SCREEN
@Composable
fun EditSongScreen(viewModel: WorshipViewModel, songId: Long?) {
    val songList by viewModel.songs.collectAsState()
    val existingSong = remember(songList, songId) { songList.find { it.id == songId } }

    val categories by viewModel.categories.collectAsState()

    var title by remember { mutableStateOf(existingSong?.title ?: "") }
    var category by remember { mutableStateOf(existingSong?.category ?: "Coros") }
    var key by remember { mutableStateOf(existingSong?.key ?: "C") }
    var lyrics by remember { mutableStateOf(existingSong?.lyrics ?: "") }
    var notes by remember { mutableStateOf(existingSong?.notes ?: "") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Nueva Categoría") },
            text = {
                Column {
                    Text("Ingresa el nombre de la nueva categoría:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleaned = newCategoryName.trim()
                        if (cleaned.isNotEmpty()) {
                            viewModel.addCategory(cleaned)
                            category = cleaned
                        }
                        showAddCategoryDialog = false
                    },
                    enabled = newCategoryName.trim().isNotEmpty()
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (songId == null) "Nueva Canción" else "Editar Canción",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (existingSong != null) {
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                        }

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Eliminar Canción") },
                                text = { Text("¿Estás seguro de que quieres eliminar '${existingSong.title}'?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deleteSong(existingSong)
                                        showDeleteConfirm = false
                                    }) {
                                        Text("Eliminar", color = Color.Red)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("Cancelar")
                                    }
                                }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (title.trim().isNotEmpty() && lyrics.trim().isNotEmpty()) {
                                val song = Song(
                                    id = existingSong?.id ?: 0L,
                                    title = title,
                                    category = category,
                                    key = key,
                                    lyrics = lyrics,
                                    notes = notes,
                                    isFavorite = existingSong?.isFavorite ?: false,
                                    dateCreated = existingSong?.dateCreated ?: System.currentTimeMillis()
                                )
                                viewModel.saveSong(song) {
                                    viewModel.navigateBack()
                                }
                            }
                        },
                        enabled = title.trim().isNotEmpty() && lyrics.trim().isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_save_song")
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la canción") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_title")
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = {
                                IconButton(onClick = { categoryExpanded = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ Agregar categoría...", color = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    newCategoryName = ""
                                    showAddCategoryDialog = true
                                    categoryExpanded = false
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("Tono") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.5f)
                            .testTag("input_key")
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    label = { Text("Letra de la canción") },
                    placeholder = { Text("Ej:\n[G]Jehová es mi [C]pastor\n[D]nada me fal[G]tará...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .testTag("input_lyrics")
                )
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas o apuntes para el grupo") },
                    placeholder = { Text("Ej: Subir intensidad en el puente") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }
        }
    }
}


// 5. WORSHIP MODE SCREEN (MODO DE CANTO)
@Composable
fun WorshipModeScreen(
    viewModel: WorshipViewModel,
    songId: Long,
    mosaicId: Long?,
    mosaicSongIndex: Int
) {
    val context = LocalContext.current
    val songList by viewModel.songs.collectAsState()
    val song = remember(songList, songId) { songList.find { it.id == songId } }

    var showChords by remember { mutableStateOf(viewModel.showChordsByDefault) }
    var currentFontSize by remember { mutableStateOf(viewModel.worshipFontSize) }
    val isSessionActive by viewModel.isSessionActive.collectAsState()

    // Floating Leader menu control toggle
    var showLeaderMenu by remember { mutableStateOf(false) }

    // Keep screen awake side-effect
    DisposableEffect(viewModel.keepScreenAwake) {
        val activity = context as? Activity
        if (viewModel.keepScreenAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (song == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cargando canción...")
        }
        return
    }

    val isDark = viewModel.isDarkMode
    val backgroundColor = if (isDark) Color.Black else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1C1B20)
    val subtextColor = if (isDark) Color.LightGray else Color(0xFF625B71)
    val dividerColor = if (isDark) Color.DarkGray else Color(0xFFE0E0E0)
    val mosaicBarColor = if (isDark) Color(0xFF121212) else Color(0xFFF3EDF7)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = textColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = song.title,
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Tono: ${song.key} • ${song.category}",
                            color = subtextColor,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Font adjustment buttons
                    IconButton(onClick = {
                        currentFontSize = (currentFontSize - 2f).coerceAtLeast(14f)
                        viewModel.updateWorshipFontSize(currentFontSize)
                    }) {
                        Text("A-", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    IconButton(onClick = {
                        currentFontSize = (currentFontSize + 2f).coerceAtMost(50f)
                        viewModel.updateWorshipFontSize(currentFontSize)
                    }) {
                        Text("A+", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    // Chord display toggle button
                    IconButton(onClick = { showChords = !showChords }) {
                        Icon(
                            imageVector = if (showChords) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Ver Acordes",
                            tint = textColor
                        )
                    }
                }
            }

            Divider(color = dividerColor)

            // Lyrics Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    val lines = song.lyrics.split("\n")
                    items(lines) { line ->
                        LyricLineView(line = line, showChords = showChords, fontSize = currentFontSize, isDark = isDark)
                    }
                }
            }

            // Mosaic bottom navigation if any
            if (mosaicId != null) {
                val mosaicList by viewModel.mosaics.collectAsState()
                val activeMosaic = remember(mosaicList, mosaicId) { mosaicList.find { it.id == mosaicId } }
                if (activeMosaic != null) {
                    val songIds = remember(activeMosaic) {
                        activeMosaic.songIds.split(",").mapNotNull { it.toLongOrNull() }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(mosaicBarColor)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            enabled = mosaicSongIndex > 0,
                            onClick = {
                                val prevIndex = mosaicSongIndex - 1
                                val prevSongId = songIds[prevIndex]
                                viewModel.navigateTo(
                                    Screen.WorshipMode(prevSongId, mosaicId, prevIndex),
                                    addToBackStack = false
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Anterior",
                                tint = if (mosaicSongIndex > 0) textColor else dividerColor
                            )
                        }

                        Text(
                            text = "${mosaicSongIndex + 1} de ${songIds.size} • Mosaico: ${activeMosaic.name}",
                            color = subtextColor,
                            fontSize = 14.sp
                        )

                        IconButton(
                            enabled = mosaicSongIndex < songIds.size - 1,
                            onClick = {
                                val nextIndex = mosaicSongIndex + 1
                                val nextSongId = songIds[nextIndex]
                                viewModel.navigateTo(
                                    Screen.WorshipMode(nextSongId, mosaicId, nextIndex),
                                    addToBackStack = false
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Siguiente",
                                tint = if (mosaicSongIndex < songIds.size - 1) textColor else dividerColor
                            )
                        }
                    }
                }
            }
        }

        // Floating Leader Control Menu Button (Only visible if Live Session is active/running on leader device!)
        if (isSessionActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = { showLeaderMenu = !showLeaderMenu },
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White,
                    modifier = Modifier.testTag("btn_floating_leader_bar")
                ) {
                    Icon(
                        imageVector = if (showLeaderMenu) Icons.Default.Close else Icons.Default.Campaign,
                        contentDescription = "Comandos de Director"
                    )
                }
            }
        }

        // Expanding Leader Floating Control Overlay
        if (showLeaderMenu && isSessionActive) {
            Dialog(onDismissRequest = { showLeaderMenu = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E1E24) else Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "👑 Comandos del Director",
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Presiona para enviar un mensaje instantáneo al equipo",
                            color = subtextColor,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions Grid
                        val commands = listOf(
                            "🔵 REPETIMOS CORO", "Estrofa 1", "Estrofa 2",
                            "Estrofa 3", "Puente", "Coro", "Final",
                            "🟢 SOLO PIANO", "SOLO GUITARRA", "🟡 TODA LA BANDA",
                            "Esperar", "Improvisar", "🔴 TERMINAMOS"
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(300.dp)
                        ) {
                            items(commands) { cmd ->
                                Button(
                                    onClick = {
                                        viewModel.broadcastOverlayCommand(cmd)
                                        showLeaderMenu = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            cmd.contains("REPETIMOS") || cmd.contains("Coro") -> Color(0xFF1565C0)
                                            cmd.contains("SOLO") -> Color(0xFF2E7D32)
                                            cmd.contains("BANDA") -> Color(0xFFEF6C00)
                                            cmd.contains("TERMINAMOS") || cmd.contains("Final") -> Color(0xFFC62828)
                                            else -> if (isDark) Color(0xFF37474F) else Color(0xFF546E7A)
                                        }
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text(cmd, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showLeaderMenu = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cerrar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricLineView(
    line: String,
    showChords: Boolean,
    fontSize: Float,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val chordColor = if (isDark) Color(0xFFFFD23F) else Color(0xFF1A237E) // Deep Indigo Blue for high contrast in light mode
    val textColor = if (isDark) Color.White else Color(0xFF1C1B20) // Crisp dark black text on white background in light mode
    val headerColor = if (isDark) Color(0xFFFF9800) else Color(0xFFD84315) // Deep Orange-Red for section headers in light mode

    if (showChords) {
        val parsed = remember(line) { parseChordLine(line) }
        if (parsed != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Render Chords
                Text(
                    text = parsed.first,
                    fontSize = (fontSize * 0.85f).sp,
                    fontFamily = FontFamily.Monospace,
                    color = chordColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                // Render text line in monospace for pixel-perfect vertical alignment under the chords
                Text(
                    text = parsed.second,
                    fontSize = fontSize.sp,
                    fontFamily = FontFamily.Monospace,
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return
        }
    }

    // Default clean text rendering (chords stripped)
    val cleanLine = remember(line) { line.replace(Regex("\\[.*?\\]"), "") }
    val isChorusHeader = cleanLine.startsWith("Coro:") || cleanLine.startsWith("Estrofa") || cleanLine.startsWith("Puente") || cleanLine.startsWith("Pre-coro")
    
    Text(
        text = cleanLine,
        fontSize = fontSize.sp,
        color = if (isChorusHeader) headerColor else textColor,
        fontWeight = if (isChorusHeader) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isChorusHeader) 10.dp else 4.dp)
    )
}


// 6. MOSAIC LIST SCREEN
@Composable
fun MosaicsScreen(viewModel: WorshipViewModel) {
    val mosaicList by viewModel.mosaics.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mosaicos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.CreateMosaic()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_mosaic")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Crear Mosaico")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (mosaicList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(imageVector = Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No tienes mosaicos creados.", color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Crea listas ordenadas de canciones para usarlas durante el servicio.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(mosaicList) { mosaic ->
                        MosaicRowItem(mosaic = mosaic, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MosaicRowItem(mosaic: Mosaic, viewModel: WorshipViewModel) {
    val songList by viewModel.songs.collectAsState()
    val songIds = remember(mosaic) {
        mosaic.songIds.split(",").mapNotNull { it.toLongOrNull() }
    }
    val firstSongId = songIds.firstOrNull()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (firstSongId != null) {
                    viewModel.navigateTo(Screen.WorshipMode(firstSongId, mosaic.id, 0))
                }
            }
            .testTag("mosaic_item_${mosaic.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mosaic.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${songIds.size} canciones en total",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Row {
                IconButton(onClick = { viewModel.deleteMosaic(mosaic) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                }
                IconButton(onClick = {
                    if (firstSongId != null) {
                        viewModel.navigateTo(Screen.WorshipMode(firstSongId, mosaic.id, 0))
                    }
                }) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Iniciar", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}


// 7. CREATE MOSAIC SCREEN
@Composable
fun CreateMosaicScreen(viewModel: WorshipViewModel) {
    val songList by viewModel.songs.collectAsState()
    var mosaicName by remember { mutableStateOf("") }
    val selectedSongIds = remember { mutableStateListOf<Long>() }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nuevo Mosaico",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        if (mosaicName.trim().isNotEmpty() && selectedSongIds.isNotEmpty()) {
                            viewModel.saveMosaic(mosaicName, selectedSongIds) {
                                viewModel.navigateBack()
                            }
                        }
                    },
                    enabled = mosaicName.trim().isNotEmpty() && selectedSongIds.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_save_mosaic")
                ) {
                    Text("Guardar")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            OutlinedTextField(
                value = mosaicName,
                onValueChange = { mosaicName = it },
                label = { Text("Nombre del Mosaico") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_mosaic_name")
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Selecciona las canciones (${selectedSongIds.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (songList.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("No hay canciones para agregar. Crea algunas primero.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(songList) { song ->
                        val isSelected = selectedSongIds.contains(song.id)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        selectedSongIds.remove(song.id)
                                    } else {
                                        selectedSongIds.add(song.id)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(song.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("${song.category} • Tono: ${song.key}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) {
                                            selectedSongIds.remove(song.id)
                                        } else {
                                            selectedSongIds.add(song.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// 8. FAVORITES SCREEN
@Composable
fun FavoritesScreen(viewModel: WorshipViewModel) {
    val favoriteList by viewModel.favorites.collectAsState()

    val listState = rememberLazyListState()
    var isScrollRestored by remember { mutableStateOf(false) }

    LaunchedEffect(favoriteList) {
        if (favoriteList.isNotEmpty() && !isScrollRestored) {
            if (viewModel.favoritesScrollIndex < favoriteList.size) {
                listState.scrollToItem(viewModel.favoritesScrollIndex, viewModel.favoritesScrollOffset)
            }
            isScrollRestored = true
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            .collect { (index, offset) ->
                if (isScrollRestored) {
                    viewModel.favoritesScrollIndex = index
                    viewModel.favoritesScrollOffset = offset
                }
            }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Favoritos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (favoriteList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(imageVector = Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Aún no tienes canciones favoritas.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(favoriteList, key = { it.id }) { song ->
                        SongRowItem(
                            song = song,
                            onClick = {
                                viewModel.navigateTo(Screen.SongDetail(song.id))
                            },
                            onFavoriteToggle = {
                                viewModel.toggleFavorite(song)
                            },
                            onCategoryClick = { cat -> viewModel.selectCategoryFilter(cat) },
                            onKeyClick = { key -> viewModel.selectKeyFilter(key) }
                        )
                    }
                }
            }
        }
    }
}


// 9. SEARCH SCREEN
@Composable
fun SearchScreen(viewModel: WorshipViewModel) {
    val songList by viewModel.songs.collectAsState()
    var query by remember { mutableStateOf(viewModel.searchQuery) }

    LaunchedEffect(query) {
        viewModel.searchQuery = query
    }

    val searchResults = remember(songList, query) {
        if (query.trim().isEmpty()) {
            emptyList()
        } else {
            songList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.lyrics.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.key.contains(query, ignoreCase = true)
            }
        }
    }

    val listState = rememberLazyListState()
    var isScrollRestored by remember { mutableStateOf(false) }

    LaunchedEffect(searchResults) {
        if (searchResults.isNotEmpty() && !isScrollRestored) {
            if (viewModel.searchScrollIndex < searchResults.size) {
                listState.scrollToItem(viewModel.searchScrollIndex, viewModel.searchScrollOffset)
            }
            isScrollRestored = true
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            .collect { (index, offset) ->
                if (isScrollRestored) {
                    viewModel.searchScrollIndex = index
                    viewModel.searchScrollOffset = offset
                }
            }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar por título, letra, tono...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                        .testTag("search_input_field")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (query.trim().isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Escribe algo para comenzar a buscar", color = Color.Gray)
                }
            } else if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron resultados para '$query'", color = Color.Gray)
                }
            } else {
                Text(
                    text = "Resultados (${searchResults.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(searchResults, key = { it.id }) { song ->
                        SongRowItem(
                            song = song,
                            onClick = {
                                viewModel.navigateTo(Screen.SongDetail(song.id))
                            },
                            onFavoriteToggle = {
                                viewModel.toggleFavorite(song)
                            },
                            onCategoryClick = { cat -> viewModel.selectCategoryFilter(cat) },
                            onKeyClick = { key -> viewModel.selectKeyFilter(key) }
                        )
                    }
                }
            }
        }
    }
}


// 10. SESSION LEADER SCREEN (MODO DIRECTOR)
@Composable
fun SessionLeaderScreen(viewModel: WorshipViewModel) {
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val connectedCount by viewModel.connectedClientsCount.collectAsState()
    val commands by viewModel.commands.collectAsState()
    var enteredPassword by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    var showCommandManager by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var newCommandText by remember { mutableStateOf("") }
    var editingCommand by remember { mutableStateOf<com.example.data.model.WorshipCommand?>(null) }
    var editingCommandText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Modo Director (Sesión)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            if (!isUnlocked && !isSessionActive) {
                // Password lock screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Acceso Restringido", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Ingresa la contraseña del Director", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = enteredPassword,
                        onValueChange = {
                            enteredPassword = it
                            passwordError = false
                        },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        isError = passwordError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    if (passwordError) {
                        Text("Contraseña incorrecta. Intenta nuevamente.", color = Color.Red, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (enteredPassword == viewModel.leaderPassword) {
                                isUnlocked = true
                            } else {
                                passwordError = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_unlock_director")
                    ) {
                        Text("Desbloquear")
                    }
                }
            } else {
                // Session Leader active panel
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSessionActive) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSessionActive) Color.Green else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSessionActive) "SESIÓN ACTIVA" else "SESIÓN INACTIVA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSessionActive) Color(0xFF2E7D32) else Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (isSessionActive) {
                                Text("IP del Director: ${viewModel.localIpAddress}", fontWeight = FontWeight.Bold)
                                Text("Puerto de Sincronización: 9876", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Integrantes conectados: $connectedCount", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Inicia la sesión para transmitir canciones y comandos en vivo a todo tu equipo conectado a la red local.")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (isSessionActive) {
                                        viewModel.stopWorshipSession()
                                    } else {
                                        viewModel.startWorshipSession()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSessionActive) Color.Red else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_toggle_session")
                            ) {
                                Text(if (isSessionActive) "Detener Sesión" else "Iniciar Sesión")
                            }
                        }
                    }

                    if (isSessionActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enviar Comandos Directos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { showCommandManager = true },
                                modifier = Modifier.testTag("btn_personalizar_comandos")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Personalizar", fontSize = 13.sp)
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(1),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(commands) { cmdObj ->
                                val cmd = cmdObj.text
                                Button(
                                    onClick = { viewModel.broadcastOverlayCommand(cmd) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            cmd.startsWith("🔵") || cmd.uppercase().contains("REPETIMOS") || cmd.uppercase().contains("CORO") -> Color(0xFF1565C0)
                                            cmd.startsWith("🟢") || cmd.uppercase().contains("PIANO") || cmd.uppercase().contains("SOLO") -> Color(0xFF2E7D32)
                                            cmd.startsWith("🟡") || cmd.uppercase().contains("BANDA") -> Color(0xFFEF6C00)
                                            cmd.startsWith("🔴") || cmd.uppercase().contains("TERMINAMOS") || cmd.uppercase().contains("FIN") -> Color(0xFFC62828)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        contentColor = when {
                                            cmd.startsWith("🔵") || cmd.uppercase().contains("REPETIMOS") || cmd.uppercase().contains("CORO") ||
                                            cmd.startsWith("🟢") || cmd.uppercase().contains("PIANO") || cmd.uppercase().contains("SOLO") ||
                                            cmd.startsWith("🟡") || cmd.uppercase().contains("BANDA") ||
                                            cmd.startsWith("🔴") || cmd.uppercase().contains("TERMINAMOS") || cmd.uppercase().contains("FIN") -> Color.White
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("cmd_button_${cmdObj.id}")
                                ) {
                                    Text(cmd, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (showCommandManager) {
                        AlertDialog(
                            onDismissRequest = { showCommandManager = false },
                            title = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Personalizar Comandos",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(onClick = {
                                        viewModel.resetCommandsToDefault()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Restablecer predeterminados",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 380.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Agrega, edita, reordena o elimina los comandos disponibles para el equipo.",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                    if (commands.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No hay comandos creados.", color = Color.Gray)
                                        }
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f, fill = false)
                                        ) {
                                            itemsIndexed(commands) { index, cmd ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = cmd.text,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(horizontal = 4.dp),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    IconButton(
                                                        onClick = { viewModel.moveCommandUp(cmd) },
                                                        enabled = index > 0,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowUpward,
                                                            contentDescription = "Subir",
                                                            modifier = Modifier.size(18.dp),
                                                            tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.moveCommandDown(cmd) },
                                                        enabled = index < commands.size - 1,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDownward,
                                                            contentDescription = "Bajar",
                                                            modifier = Modifier.size(18.dp),
                                                            tint = if (index < commands.size - 1) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            editingCommand = cmd
                                                            editingCommandText = cmd.text
                                                            showEditDialog = true
                                                        },
                                                        modifier = Modifier.size(32.dp).testTag("btn_edit_cmd_${cmd.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Editar",
                                                            modifier = Modifier.size(18.dp),
                                                            tint = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.deleteCommand(cmd) },
                                                        modifier = Modifier.size(32.dp).testTag("btn_delete_cmd_${cmd.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Eliminar",
                                                            modifier = Modifier.size(18.dp),
                                                            tint = Color.Red.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = {
                                            newCommandText = ""
                                            showAddDialog = true
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("btn_agregar_nuevo_comando_trigger")
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Agregar Comando")
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = { showCommandManager = false },
                                    modifier = Modifier.testTag("btn_cerrar_manager")
                                ) {
                                    Text("Cerrar")
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    if (showAddDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddDialog = false },
                            title = { Text("Agregar Comando", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Escribe el texto para el nuevo botón. Puedes usar emojis al inicio para darle color (🔵, 🟢, 🟡, 🔴).", fontSize = 12.sp, color = Color.Gray)
                                    OutlinedTextField(
                                        value = newCommandText,
                                        onValueChange = { newCommandText = it },
                                        placeholder = { Text("Ej: 🔵 SOLO BAJO") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("input_new_cmd_text")
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (newCommandText.isNotBlank()) {
                                            viewModel.addCommand(newCommandText.trim())
                                            showAddDialog = false
                                        }
                                    },
                                    enabled = newCommandText.isNotBlank(),
                                    modifier = Modifier.testTag("btn_save_new_cmd")
                                ) {
                                    Text("Guardar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddDialog = false }) {
                                    Text("Cancelar")
                                }
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    if (showEditDialog) {
                        val currentCmd = editingCommand
                        if (currentCmd != null) {
                            AlertDialog(
                                onDismissRequest = { showEditDialog = false },
                                title = { Text("Editar Comando", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Escribe el nuevo texto para el comando.", fontSize = 12.sp, color = Color.Gray)
                                        OutlinedTextField(
                                            value = editingCommandText,
                                            onValueChange = { editingCommandText = it },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("input_edit_cmd_text")
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (editingCommandText.isNotBlank()) {
                                                viewModel.updateCommand(currentCmd.copy(text = editingCommandText.trim()))
                                                showEditDialog = false
                                            }
                                        },
                                        enabled = editingCommandText.isNotBlank(),
                                        modifier = Modifier.testTag("btn_save_edit_cmd")
                                    ) {
                                        Text("Actualizar")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showEditDialog = false }) {
                                        Text("Cancelar")
                                    }
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


// 11. SESSION MEMBER SCREEN (MODO INTEGRANTE)
@Composable
fun SessionMemberScreen(viewModel: WorshipViewModel) {
    val memberStatus by viewModel.memberStatus.collectAsState()
    val discoveredLeaders by viewModel.discoveredLeaders.collectAsState()
    var manualIp by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.startDiscoveringLeaders()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopDiscoveringLeaders()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Modo Integrante",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (memberStatus) {
                            com.example.network.WorshipClient.ConnectionStatus.CONNECTED -> Color(0xFF1565C0).copy(alpha = 0.15f)
                            com.example.network.WorshipClient.ConnectionStatus.CONNECTING -> Color(0xFFFF9800).copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = when (memberStatus) {
                                com.example.network.WorshipClient.ConnectionStatus.CONNECTED -> "🔵 CONECTADO AL DIRECTOR"
                                com.example.network.WorshipClient.ConnectionStatus.CONNECTING -> "🟡 CONECTANDO..."
                                else -> "🔴 NO CONECTADO"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = when (memberStatus) {
                                com.example.network.WorshipClient.ConnectionStatus.CONNECTED -> Color(0xFF1565C0)
                                com.example.network.WorshipClient.ConnectionStatus.CONNECTING -> Color(0xFFFF9800)
                                else -> Color.Red
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("En esta pantalla puedes unirte a la sesión del Director de tu iglesia. Una vez conectado, tu app cargará automáticamente las canciones elegidas por el director y mostrará mensajes en pantalla completa en tiempo real.")

                        if (memberStatus == com.example.network.WorshipClient.ConnectionStatus.CONNECTED) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.disconnectFromLeader() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Desconectar")
                            }
                        }
                    }
                }
            }

            if (memberStatus != com.example.network.WorshipClient.ConnectionStatus.CONNECTED) {
                item {
                    Text("Directores en la Red (Buscando...)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                if (discoveredLeaders.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Buscando directores activos...", fontSize = 13.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                } else {
                    items(discoveredLeaders) { leader ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.connectToLeader(leader.second)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(leader.first, fontWeight = FontWeight.Bold)
                                    Text("IP: ${leader.second}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }

                item {
                    Text("Conexión Manual", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = manualIp,
                                onValueChange = { manualIp = it },
                                label = { Text("IP del Director (Ej: 192.168.1.100)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_ip_input")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (manualIp.trim().isNotEmpty()) {
                                        viewModel.connectToLeader(manualIp.trim())
                                    }
                                },
                                enabled = manualIp.trim().isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_connect_manual")
                            ) {
                                Text("Conectar por IP")
                            }
                        }
                    }
                }
            }
        }
    }
}


// 12. IMPORT / EXPORT SCREEN
@Composable
fun ImportExportScreen(viewModel: WorshipViewModel) {
    val context = LocalContext.current
    val importStats by remember { derivedStateOf { viewModel.importStats } }
    var showImportConfirm by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.analyzeImportFile(context, uri)
            showImportConfirm = true
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Importar / Exportar",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Comparte tu cancionero con el resto de integrantes de manera manual, sin necesidad de cuentas ni servidores en la nube.")

            // Export Box
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Exportar Cancionero", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Genera un archivo portátil '.alabanza' que contiene todas tus canciones y envíalo por WhatsApp, Telegram, Bluetooth o correo.")
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.exportSongbook(context) { file ->
                                val fileUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, fileUri)
                                    type = "application/octet-stream"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Exportar Cancionero")
                                context.startActivity(shareIntent)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_exportar")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Cancionero")
                    }
                }
            }

            // Import Box
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Importar Cancionero", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Selecciona un archivo '.alabanza' recibido para agregar nuevas canciones o actualizar las existentes de forma inteligente.")
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_importar")
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleccionar Archivo")
                    }
                }
            }
        }

        // Smart Import Confirmation Dialog
        if (showImportConfirm && importStats != null) {
            val stats = importStats!!
            AlertDialog(
                onDismissRequest = {
                    viewModel.clearImportState()
                    showImportConfirm = false
                },
                title = { Text("📊 Análisis de Importación Inteligente") },
                text = {
                    Column {
                        Text("Se analizó el archivo correctamente:")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("• Canciones Nuevas: ${stats.newCount}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Text("• Canciones con actualizaciones: ${stats.updatedCount}", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        Text("• Canciones duplicadas: ${stats.duplicateCount}", color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("¿Deseas completar la importación? Las canciones duplicadas se ignorarán automáticamente.")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.confirmImport(overwriteExisting = true) {
                                showImportConfirm = false
                            }
                        }
                    ) {
                        Text("Confirmar e Importar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearImportState()
                            showImportConfirm = false
                        }
                    ) {
                        Text("Cancelar", color = Color.Red)
                    }
                }
            )
        }
    }
}


// 13. SETTINGS SCREEN
@Composable
fun SettingsScreen(viewModel: WorshipViewModel) {
    var passwordInput by remember { mutableStateOf(viewModel.leaderPassword) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configuración",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dark Theme Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tema Oscuro", fontWeight = FontWeight.Bold)
                    Text("Fondo oscuro para mayor legibilidad", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(
                    checked = viewModel.isDarkMode,
                    onCheckedChange = { viewModel.updateDarkMode(it) },
                    modifier = Modifier.testTag("switch_dark_mode")
                )
            }

            Divider()

            // Keep Screen Awake Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Mantener Pantalla Activa", fontWeight = FontWeight.Bold)
                    Text("Evita que la pantalla se apague al cantar", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(
                    checked = viewModel.keepScreenAwake,
                    onCheckedChange = { viewModel.updateKeepScreenAwake(it) }
                )
            }

            Divider()

            // Chords by Default Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Mostrar Acordes por Defecto", fontWeight = FontWeight.Bold)
                    Text("Carga las letras con acordes automáticamente", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(
                    checked = viewModel.showChordsByDefault,
                    onCheckedChange = { viewModel.updateShowChordsByDefault(it) }
                )
            }

            Divider()

            // Haptic Feedback Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vibración por comandos", fontWeight = FontWeight.Bold)
                    Text("Vibrar brevemente al recibir cambios del director", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(
                    checked = viewModel.hapticFeedbackEnabled,
                    onCheckedChange = { viewModel.updateHapticFeedbackEnabled(it) },
                    modifier = Modifier.testTag("switch_haptic_feedback")
                )
            }

            Divider()

            // Password customization
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Contraseña del Director", fontWeight = FontWeight.Bold)
                Text("Evita que integrantes inicien sesión como directores", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        singleLine = true,
                        placeholder = { Text("1234") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_password_input")
                    )

                    Button(
                        onClick = {
                            if (passwordInput.trim().isNotEmpty()) {
                                viewModel.updateLeaderPassword(passwordInput.trim())
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar")
                    }
                }
            }

            val context = LocalContext.current
            var showRestoreConfirmDialog by remember { mutableStateOf<android.net.Uri?>(null) }
            var showImportConfirm by remember { mutableStateOf(false) }
            val importStats by remember { derivedStateOf { viewModel.importStats } }

            val backupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
                onResult = { uri ->
                    uri?.let {
                        val outputStream = context.contentResolver.openOutputStream(it)
                        if (outputStream != null) {
                            viewModel.backupDatabase(
                                outputStream = outputStream,
                                onSuccess = {
                                    android.widget.Toast.makeText(context, "✅ Copia de seguridad guardada con éxito", android.widget.Toast.LENGTH_LONG).show()
                                },
                                onError = { error ->
                                    android.widget.Toast.makeText(context, "❌ $error", android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            android.widget.Toast.makeText(context, "❌ No se pudo abrir el archivo de destino", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            val restoreLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = { uri ->
                    uri?.let {
                        showRestoreConfirmDialog = it
                    }
                }
            )

            val jsonBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json"),
                onResult = { uri ->
                    uri?.let {
                        val outputStream = context.contentResolver.openOutputStream(it)
                        if (outputStream != null) {
                            viewModel.exportSongbookJson(
                                outputStream = outputStream,
                                onSuccess = {
                                    android.widget.Toast.makeText(context, "✅ Cancionero exportado en formato JSON con éxito", android.widget.Toast.LENGTH_LONG).show()
                                },
                                onError = { error ->
                                    android.widget.Toast.makeText(context, "❌ $error", android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            android.widget.Toast.makeText(context, "❌ No se pudo abrir el archivo de destino", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            val jsonRestoreLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri ->
                    uri?.let {
                        viewModel.analyzeImportFile(context, it)
                        showImportConfirm = true
                    }
                }
            )

            if (showRestoreConfirmDialog != null) {
                AlertDialog(
                    onDismissRequest = { showRestoreConfirmDialog = null },
                    title = { Text("¿Restaurar Base de Datos?") },
                    text = {
                        Text("Esto reemplazará todas las canciones, mosaicos e indicaciones actuales. La aplicación se reiniciará automáticamente para aplicar los cambios.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val uri = showRestoreConfirmDialog
                                showRestoreConfirmDialog = null
                                if (uri != null) {
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    if (inputStream != null) {
                                        viewModel.restoreDatabase(
                                            inputStream = inputStream,
                                            onSuccess = {
                                                android.widget.Toast.makeText(context, "✅ Restauración completa. Reiniciando...", android.widget.Toast.LENGTH_LONG).show()
                                                restartApp(context)
                                            },
                                            onError = { error ->
                                                android.widget.Toast.makeText(context, "❌ $error", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        android.widget.Toast.makeText(context, "❌ No se pudo leer el archivo de origen", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Confirmar y Reiniciar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRestoreConfirmDialog = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            if (showImportConfirm && importStats != null) {
                val stats = importStats!!
                AlertDialog(
                    onDismissRequest = {
                        viewModel.clearImportState()
                        showImportConfirm = false
                    },
                    title = { Text("📊 Análisis de Importación Inteligente") },
                    text = {
                        Column {
                            Text("Se analizó el archivo correctamente:")
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("• Canciones Nuevas: ${stats.newCount}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("• Canciones con actualizaciones: ${stats.updatedCount}", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                            Text("• Canciones duplicadas: ${stats.duplicateCount}", color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Selecciona el método de importación para evitar duplicados:")
                        }
                    },
                    confirmButton = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.confirmImport(overwriteExisting = true) {
                                        showImportConfirm = false
                                        android.widget.Toast.makeText(context, "✅ Importación exitosa (nuevas y actualizadas)", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("import_confirm_overwrite")
                            ) {
                                Text("Añadir Nuevas y Actualizar Existentes", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.confirmImport(overwriteExisting = false) {
                                        showImportConfirm = false
                                        android.widget.Toast.makeText(context, "✅ Importación exitosa (solo nuevas)", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("import_confirm_new_only")
                            ) {
                                Text("Solo Añadir Nuevas (Evitar Duplicados)", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = {
                                    viewModel.clearImportState()
                                    showImportConfirm = false
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Cancelar", color = Color.Red)
                            }
                        }
                    }
                )
            }

            Divider()

            // 1. Full Database Backup / Restore Card (.db)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("backup_restore_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Base de Datos Completa (.db)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "Guarda una copia exacta de toda la base de datos (canciones, mosaicos, indicaciones, categorías y configuraciones) en un archivo .db, o restaura una copia existente.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                backupLauncher.launch("cancionero_respaldo.db")
                            },
                            modifier = Modifier.weight(1f).testTag("backup_db_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Respaldar", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                restoreLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.weight(1f).testTag("restore_db_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar", fontSize = 13.sp)
                        }
                    }
                }
            }

            // 2. JSON Songs Import / Export Card (.json)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("songs_json_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Exportar/Importar Canciones (.json)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "Exporta tu cancionero en formato JSON para editar o compartir, o importa nuevas canciones de forma inteligente comparando entradas y evitando duplicados.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                jsonBackupLauncher.launch("cancionero_export.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f).testTag("export_json_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar JSON", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                jsonRestoreLauncher.launch("application/json")
                            },
                            modifier = Modifier.weight(1f).testTag("import_json_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar JSON", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun restartApp(context: android.content.Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.let {
        it.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(it)
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                currentContext.finish()
                break
            }
            currentContext = currentContext.baseContext
        }
        java.lang.System.exit(0)
    }
}

// TODAY LIST SCREEN
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TodayListScreen(viewModel: WorshipViewModel) {
    val songs by viewModel.songs.collectAsState()
    val todaySongIds by viewModel.todaySongIds.collectAsState()
    val isSessionActive by viewModel.isSessionActive.collectAsState()

    val todaySongs = remember(todaySongIds, songs) {
        todaySongIds.mapNotNull { id -> songs.find { it.id == id } }
    }

    var showClearDialog by remember { mutableStateOf(false) }
    var customListName by remember { mutableStateOf("") }

    val presetNames = listOf(
        "Culto Dominical",
        "Escuela Dominical",
        "Culto de Alabanza",
        "Ensayo de Jóvenes",
        "Reunión de Oración",
        "Culto Especial"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lista del Día", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${todaySongs.size} canción${if (todaySongs.size == 1) "" else "es"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.TodayListHistoryScreen) },
                        modifier = Modifier.testTag("btn_history_today_list")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Historial")
                    }
                    IconButton(onClick = { viewModel.navigateTo(Screen.SongBook) }) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = "Cancionero")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lista para el Culto / Ensayo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isSessionActive) "Sincronizado en vivo con los integrantes" else "Selecciona canciones rápidamente para hoy",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            if (todaySongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "La Lista del Día está vacía",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ve al Cancionero y presiona el botón '+' o de lista en cualquier canción para agregarla.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.navigateTo(Screen.SongBook) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Explorar Cancionero")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(todaySongs, key = { _, song -> song.id }) { index, song ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.navigateTo(Screen.WorshipMode(song.id))
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Tono: ${song.key}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "• ${song.category}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (index > 0) {
                                        IconButton(onClick = { viewModel.moveInTodayList(index, index - 1) }) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir")
                                        }
                                    }
                                    if (index < todaySongs.size - 1) {
                                        IconButton(onClick = { viewModel.moveInTodayList(index, index + 1) }) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar")
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeFromTodayList(song.id) },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Quitar")
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                customListName = ""
                                showClearDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_limpiar_lista")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Limpiar Lista", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                if (todaySongs.isNotEmpty()) {
                                    viewModel.navigateTo(Screen.WorshipMode(todaySongs.first().id))
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Iniciar Modo Culto", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // CLEAR / SAVE DIALOG
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                icon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Guardar e Iniciar Nueva Lista") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Escribe un nombre personalizado para guardar esta lista en el Historial de la banda antes de limpiarla:"
                        )

                        Text("Sugerencias:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetNames.forEach { name ->
                                FilterChip(
                                    selected = customListName == name,
                                    onClick = { customListName = name },
                                    label = { Text(name, fontSize = 12.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customListName,
                            onValueChange = { customListName = it },
                            label = { Text("Nombre personalizado") },
                            placeholder = { Text("Ej: Culto Dominical") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val nameToSave = customListName.ifBlank { "Culto de Alabanza" }
                            viewModel.clearTodayList(customName = nameToSave, saveToHistory = true) {
                                showClearDialog = false
                            }
                        }
                    ) {
                        Text("Guardar en Historial y Limpiar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearTodayList(customName = "", saveToHistory = false) {
                                showClearDialog = false
                            }
                        }
                    ) {
                        Text("Limpiar sin Guardar", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    }
}

// TODAY LIST HISTORY SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayListHistoryScreen(viewModel: WorshipViewModel) {
    val historyList by viewModel.todayListHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Listas del Día", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay historial guardado",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cuando limpies o guardes una Lista del Día, aparecerá en esta sección para que puedas revisarla o cargarla nuevamente.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(historyList, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.navigateTo(Screen.TodayListHistoryDetail(item.id))
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.dateLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (item.songTitlesSummary.isNotBlank()) item.songTitlesSummary else "Sin canciones",
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.deleteTodayListHistory(item) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar del Historial",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// TODAY LIST HISTORY DETAIL SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayListHistoryDetailScreen(viewModel: WorshipViewModel, historyId: Long) {
    val historyList by viewModel.todayListHistory.collectAsState()
    val allSongs by viewModel.songs.collectAsState()

    val historyItem = remember(historyList, historyId) {
        historyList.find { it.id == historyId }
    }

    val songIds = remember(historyItem) {
        historyItem?.songIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
    }

    val songs = remember(songIds, allSongs) {
        songIds.mapNotNull { id -> allSongs.find { it.id == id } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(historyItem?.dateLabel ?: "Detalle del Historial", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (historyItem == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Lista no encontrada")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = historyItem.dateLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${songs.size} canciones guardadas en esta reunión",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.loadHistoryAsTodayList(songIds)
                                viewModel.navigateTo(Screen.TodayList)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cargar como Lista del Día Actual")
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(songs) { index, song ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.navigateTo(Screen.WorshipMode(song.id))
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Tono: ${song.key}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "• ${song.category}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

