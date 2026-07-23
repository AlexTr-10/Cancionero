package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Mosaic
import com.example.data.model.Song
import com.example.data.model.TodayListHistory
import com.example.data.model.WorshipCommand
import com.example.data.repository.WorshipRepository
import com.example.network.IpUtils
import com.example.network.NsdHelper
import com.example.network.WorshipClient
import com.example.network.WorshipServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import android.os.Vibrator
import android.os.VibrationEffect
import java.io.File
import java.io.FileOutputStream

sealed class Screen {
    object Home : Screen()
    object SongBook : Screen()
    data class SongDetail(val songId: Long) : Screen()
    data class EditSong(val songId: Long? = null) : Screen()
    data class WorshipMode(val songId: Long, val mosaicId: Long? = null, val mosaicSongIndex: Int = 0) : Screen()
    object Mosaics : Screen()
    data class CreateMosaic(val mosaicId: Long? = null) : Screen()
    object TodayList : Screen()
    object TodayListHistoryScreen : Screen()
    data class TodayListHistoryDetail(val historyId: Long) : Screen()
    object Favorites : Screen()
    object Search : Screen()
    object SessionLeader : Screen()
    object SessionMember : Screen()
    object ImportExport : Screen()
    object Settings : Screen()
}

data class ImportStats(
    val newCount: Int,
    val updatedCount: Int,
    val duplicateCount: Int
)

class WorshipViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "WorshipViewModel"
    private val repository = WorshipRepository(application)
    private val sharedPrefs = application.getSharedPreferences("worship_prefs", Context.MODE_PRIVATE)

    // Reactive database flows
    val songs: StateFlow<List<Song>> = repository.allSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favorites: StateFlow<List<Song>> = repository.favoriteSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val mosaics: StateFlow<List<Mosaic>> = repository.allMosaics.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val commands: StateFlow<List<WorshipCommand>> = repository.allCommands.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val todayListHistory: StateFlow<List<TodayListHistory>> = repository.todayListHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Lista del Día active song IDs
    private val _todaySongIds = MutableStateFlow<List<Long>>(emptyList())
    val todaySongIds: StateFlow<List<Long>> = _todaySongIds.asStateFlow()

    // Navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()
    private val backStack = mutableListOf<Screen>()

    // Persistent UI States for Scroll position preservation
    var songBookSelectedCategory by mutableStateOf("Todas")
    var songBookSelectedKey by mutableStateOf<String?>(null)
    var songBookSortMode by mutableStateOf("Manual") // "Manual", "Título", "Tono", "Categoría"
    var songBookScrollIndex by mutableStateOf(0)
    var songBookScrollOffset by mutableStateOf(0)

    var favoritesScrollIndex by mutableStateOf(0)
    var favoritesScrollOffset by mutableStateOf(0)

    var searchQuery by mutableStateOf("")
    var searchScrollIndex by mutableStateOf(0)
    var searchScrollOffset by mutableStateOf(0)

    // Settings
    var isDarkMode by mutableStateOf(sharedPrefs.getBoolean("dark_mode", false))
        private set
    var hapticFeedbackEnabled by mutableStateOf(sharedPrefs.getBoolean("haptic_feedback_enabled", true))
        private set
    var worshipFontSize by mutableStateOf(sharedPrefs.getFloat("worship_font_size", 28f))
        private set
    var keepScreenAwake by mutableStateOf(sharedPrefs.getBoolean("keep_screen_awake", true))
        private set
    var showChordsByDefault by mutableStateOf(sharedPrefs.getBoolean("show_chords", true))
        private set
    var scrollingSpeed by mutableStateOf(sharedPrefs.getFloat("scrolling_speed", 2f)) // 1 to 5 scale
        private set
    var leaderPassword by mutableStateOf(sharedPrefs.getString("leader_password", "1234") ?: "1234")
        private set

    // Dynamic Categories list
    private val defaultCategories = listOf(
        "Coros", "Himnos", "Himnos Especiales", "Coros Infantiles",
        "Alabanzas", "Adoración", "Santa Cena", "Evangelismo", "Otros"
    )

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    // Live Worship Session (Leader Mode)
    private var worshipServer: WorshipServer? = null
    private val nsdHelper = NsdHelper(application)
    
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive = _isSessionActive.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount = _connectedClientsCount.asStateFlow()

    var localIpAddress by mutableStateOf("Desconocida")
        private set

    // Live Worship Session (Member Mode)
    private var worshipClient: WorshipClient? = null
    
    private val _memberStatus = MutableStateFlow(WorshipClient.ConnectionStatus.DISCONNECTED)
    val memberStatus = _memberStatus.asStateFlow()

    private val _discoveredLeaders = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // Name to IP
    val discoveredLeaders = _discoveredLeaders.asStateFlow()

    private val _activeOverlayMessage = MutableStateFlow<String?>(null)
    val activeOverlayMessage = _activeOverlayMessage.asStateFlow()

    // Smart Import State
    var importCandidates by mutableStateOf<List<Song>?>(null)
        private set
    var importStats by mutableStateOf<ImportStats?>(null)
        private set

    init {
        // Load categories, ensuring "Navidad" is filtered out
        val savedCategoriesStr = sharedPrefs.getString("song_categories_list", null)
        if (savedCategoriesStr != null) {
            val list = savedCategoriesStr.split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals("Navidad", ignoreCase = true) }
            _categories.value = list
        } else {
            _categories.value = defaultCategories
            sharedPrefs.edit().putString("song_categories_list", defaultCategories.joinToString("|")).apply()
        }

        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
        val savedTodaySongIdsStr = sharedPrefs.getString("today_song_ids", "") ?: ""
        if (savedTodaySongIdsStr.isNotBlank()) {
            _todaySongIds.value = savedTodaySongIdsStr.split(",").mapNotNull { it.trim().toLongOrNull() }
        }
        localIpAddress = IpUtils.getLocalIpAddress()
    }

    // Navigation Helpers
    fun navigateTo(screen: Screen, addToBackStack: Boolean = true) {
        if (addToBackStack) {
            backStack.add(_currentScreen.value)
        }
        _currentScreen.value = screen
        
        // If Leader is active and we navigate to a Song screen, broadcast song change automatically!
        if (_isSessionActive.value) {
            when (screen) {
                is Screen.WorshipMode -> {
                    broadcastSongChange(screen.songId)
                }
                is Screen.SongDetail -> {
                    broadcastSongChange(screen.songId)
                }
                else -> {}
            }
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeAt(backStack.size - 1)
            return true
        }
        return false
    }

    fun resetNavigation() {
        backStack.clear()
        _currentScreen.value = Screen.Home
    }

    // Settings Updates
    fun updateDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        sharedPrefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun updateHapticFeedbackEnabled(enabled: Boolean) {
        hapticFeedbackEnabled = enabled
        sharedPrefs.edit().putBoolean("haptic_feedback_enabled", enabled).apply()
    }

    fun updateWorshipFontSize(size: Float) {
        worshipFontSize = size
        sharedPrefs.edit().putFloat("worship_font_size", size).apply()
    }

    fun updateKeepScreenAwake(enabled: Boolean) {
        keepScreenAwake = enabled
        sharedPrefs.edit().putBoolean("keep_screen_awake", enabled).apply()
    }

    fun updateShowChordsByDefault(enabled: Boolean) {
        showChordsByDefault = enabled
        sharedPrefs.edit().putBoolean("show_chords", enabled).apply()
    }

    fun updateScrollingSpeed(speed: Float) {
        scrollingSpeed = speed
        sharedPrefs.edit().putFloat("scrolling_speed", speed).apply()
    }

    fun updateLeaderPassword(password: String) {
        leaderPassword = password
        sharedPrefs.edit().putString("leader_password", password).apply()
    }

    fun addCategory(newCategory: String) {
        val trimmed = newCategory.trim()
        if (trimmed.isNotBlank() && !_categories.value.any { it.equals(trimmed, ignoreCase = true) }) {
            val updatedList = _categories.value + trimmed
            _categories.value = updatedList
            sharedPrefs.edit().putString("song_categories_list", updatedList.joinToString("|")).apply()
        }
    }

    // CRUD Songs & Mosaics
    fun saveSong(song: Song, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (song.id == 0L) {
                repository.insertSong(song)
            } else {
                repository.updateSong(song)
            }
            onComplete()
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            repository.deleteSong(song)
            navigateBack()
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, !song.isFavorite)
        }
    }

    fun selectCategoryFilter(category: String) {
        songBookSelectedCategory = category
        if (_currentScreen.value !is Screen.SongBook) {
            navigateTo(Screen.SongBook)
        }
    }

    fun selectKeyFilter(key: String?) {
        songBookSelectedKey = key
        if (_currentScreen.value !is Screen.SongBook) {
            navigateTo(Screen.SongBook)
        }
    }

    fun updateBatchCategory(songIds: List<Long>, newCategory: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateBatchCategory(songIds, newCategory)
            onComplete()
        }
    }

    fun updateBatchKey(songIds: List<Long>, newKey: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateBatchKey(songIds, newKey)
            onComplete()
        }
    }

    fun deleteBatchSongs(songIds: List<Long>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteBatchSongs(songIds)
            onComplete()
        }
    }

    fun saveMosaic(name: String, songIds: List<Long>, onComplete: () -> Unit) {
        viewModelScope.launch {
            val commaSeparatedIds = songIds.joinToString(",")
            val mosaic = Mosaic(name = name, songIds = commaSeparatedIds)
            repository.insertMosaic(mosaic)
            onComplete()
        }
    }

    fun deleteMosaic(mosaic: Mosaic) {
        viewModelScope.launch {
            repository.deleteMosaic(mosaic)
        }
    }

    // Lista del Día Operations
    fun addToTodayList(songId: Long, showFeedback: Boolean = true) {
        val current = _todaySongIds.value.toMutableList()
        if (!current.contains(songId)) {
            current.add(songId)
            _todaySongIds.value = current
            saveTodaySongIdsToPrefs()
            broadcastTodayList()
            if (showFeedback) {
                viewModelScope.launch {
                    val song = repository.getSongById(songId)
                    val title = song?.title ?: "Canción"
                    _activeOverlayMessage.value = "➕ Añadida a Lista del Día: $title"
                    triggerHapticFeedback()
                    delay(2500)
                    if (_activeOverlayMessage.value?.contains(title) == true) {
                        _activeOverlayMessage.value = null
                    }
                }
            }
        } else {
            if (showFeedback) {
                viewModelScope.launch {
                    _activeOverlayMessage.value = "ℹ️ La canción ya está en la Lista del Día"
                    delay(2000)
                    if (_activeOverlayMessage.value?.contains("ya está") == true) {
                        _activeOverlayMessage.value = null
                    }
                }
            }
        }
    }

    fun removeFromTodayList(songId: Long) {
        val current = _todaySongIds.value.toMutableList()
        if (current.remove(songId)) {
            _todaySongIds.value = current
            saveTodaySongIdsToPrefs()
            broadcastTodayList()
        }
    }

    fun moveInTodayList(fromIndex: Int, toIndex: Int) {
        val current = _todaySongIds.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _todaySongIds.value = current
            saveTodaySongIdsToPrefs()
            broadcastTodayList()
        }
    }

    fun loadHistoryAsTodayList(songIds: List<Long>) {
        _todaySongIds.value = songIds
        saveTodaySongIdsToPrefs()
        broadcastTodayList()
        _activeOverlayMessage.value = "📋 Lista cargada como Lista del Día"
        viewModelScope.launch {
            delay(2500)
            if (_activeOverlayMessage.value?.contains("Lista cargada") == true) {
                _activeOverlayMessage.value = null
            }
        }
    }

    fun clearTodayList(customName: String, saveToHistory: Boolean = true, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (saveToHistory && _todaySongIds.value.isNotEmpty()) {
                val sdfDate = java.text.SimpleDateFormat("dd MMM", java.util.Locale("es", "ES"))
                val dateStr = sdfDate.format(java.util.Date())
                val cleanName = customName.trim().ifBlank { "Culto de Alabanza" }
                val dateLabel = "$dateStr - $cleanName"

                val allSongsList = songs.value
                val todaySongs = _todaySongIds.value.mapNotNull { id -> allSongsList.find { it.id == id } }
                val titlesSummary = todaySongs.joinToString(", ") { it.title }
                val songIdsStr = _todaySongIds.value.joinToString(",")

                val historyItem = TodayListHistory(
                    name = cleanName,
                    dateLabel = dateLabel,
                    songIds = songIdsStr,
                    songTitlesSummary = titlesSummary
                )
                repository.insertTodayListHistory(historyItem)

                // Broadcast save event if Leader
                if (_isSessionActive.value) {
                    val payload = JSONObject().apply {
                        put("type", "today_list_saved")
                        put("name", cleanName)
                        put("dateLabel", dateLabel)
                        put("songIds", songIdsStr)
                        put("songTitlesSummary", titlesSummary)
                    }.toString()
                    worshipServer?.broadcast(payload)
                }
            }

            _todaySongIds.value = emptyList()
            saveTodaySongIdsToPrefs()
            broadcastTodayList()
            onComplete()
        }
    }

    fun deleteTodayListHistory(history: TodayListHistory) {
        viewModelScope.launch {
            repository.deleteTodayListHistory(history)
        }
    }

    private fun saveTodaySongIdsToPrefs() {
        sharedPrefs.edit().putString("today_song_ids", _todaySongIds.value.joinToString(",")).apply()
    }

    fun broadcastTodayList() {
        if (_isSessionActive.value) {
            viewModelScope.launch(Dispatchers.IO) {
                val payload = JSONObject().apply {
                    put("type", "today_list_update")
                    put("songIds", _todaySongIds.value.joinToString(","))
                }.toString()
                worshipServer?.broadcast(payload)
            }
        }
    }

    // Leader Worship Session Operations
    fun startWorshipSession(serviceName: String = "Director de Alabanza") {
        if (_isSessionActive.value) return
        
        worshipServer = WorshipServer(port = 9876) { count ->
            _connectedClientsCount.value = count
            if (count > 0) {
                broadcastTodayList()
            }
        }
        worshipServer?.start()
        
        nsdHelper.registerService(9876, serviceName)
        _isSessionActive.value = true
        localIpAddress = IpUtils.getLocalIpAddress()
        broadcastTodayList()
    }

    fun stopWorshipSession() {
        if (!_isSessionActive.value) return
        
        worshipServer?.stop()
        worshipServer = null
        
        nsdHelper.unregisterService()
        _isSessionActive.value = false
        _connectedClientsCount.value = 0
    }

    fun broadcastOverlayCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put("type", "overlay")
                put("message", command)
            }.toString()
            worshipServer?.broadcast(payload)
        }
    }

    private fun broadcastSongChange(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val song = repository.getSongById(songId)
            if (song != null) {
                val payload = JSONObject().apply {
                    put("type", "song_change")
                    put("songId", song.id)
                    put("title", song.title)
                }.toString()
                worshipServer?.broadcast(payload)
            }
        }
    }

    // Member Worship Session Operations
    fun startDiscoveringLeaders() {
        _discoveredLeaders.value = emptyList()
        nsdHelper.discoverServices { name, ip, port ->
            val currentList = _discoveredLeaders.value.toMutableList()
            if (currentList.none { it.second == ip }) {
                currentList.add(Pair(name, ip))
                _discoveredLeaders.value = currentList
            }
        }
    }

    fun stopDiscoveringLeaders() {
        nsdHelper.stopDiscovery()
    }

    fun connectToLeader(ip: String) {
        disconnectFromLeader()
        worshipClient = WorshipClient(
            onMessageReceived = { message ->
                handleIncomingSessionMessage(message)
            },
            onStatusChanged = { status ->
                viewModelScope.launch(Dispatchers.Main) {
                    _memberStatus.value = status
                }
            }
        )
        worshipClient?.connect(ip, 9876)
    }

    fun disconnectFromLeader() {
        worshipClient?.disconnect()
        worshipClient = null
        _memberStatus.value = WorshipClient.ConnectionStatus.DISCONNECTED
    }

    private fun triggerHapticFeedback() {
        if (!hapticFeedbackEnabled) return
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 100, 100, 100)
                val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate: ${e.message}")
        }
    }

    private fun handleIncomingSessionMessage(rawMessage: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val json = JSONObject(rawMessage)
                when (json.getString("type")) {
                    "overlay" -> {
                        val message = json.getString("message")
                        _activeOverlayMessage.value = message
                        triggerHapticFeedback()
                        viewModelScope.launch {
                            delay(4000)
                            if (_activeOverlayMessage.value == message) {
                                _activeOverlayMessage.value = null
                            }
                        }
                    }
                    "song_change" -> {
                        val songId = json.getLong("songId")
                        val title = json.getString("title")
                        
                        // Show short alert
                        _activeOverlayMessage.value = "📖 El Director abrió: $title"
                        triggerHapticFeedback()
                        viewModelScope.launch {
                            delay(3000)
                            if (_activeOverlayMessage.value?.contains(title) == true) {
                                _activeOverlayMessage.value = null
                            }
                        }
                        
                        // Automatically jump to song detail or worship mode!
                        navigateTo(Screen.WorshipMode(songId), addToBackStack = true)
                    }
                    "today_list_update" -> {
                        val songIdsStr = json.optString("songIds", "")
                        val ids = if (songIdsStr.isNotBlank()) {
                            songIdsStr.split(",").mapNotNull { it.trim().toLongOrNull() }
                        } else {
                            emptyList()
                        }
                        _todaySongIds.value = ids
                        saveTodaySongIdsToPrefs()
                    }
                    "today_list_saved" -> {
                        val name = json.optString("name", "Culto")
                        val dateLabel = json.optString("dateLabel", "Lista del Día")
                        val songIdsStr = json.optString("songIds", "")
                        val songTitlesSummary = json.optString("songTitlesSummary", "")

                        val historyItem = TodayListHistory(
                            name = name,
                            dateLabel = dateLabel,
                            songIds = songIdsStr,
                            songTitlesSummary = songTitlesSummary
                        )
                        repository.insertTodayListHistory(historyItem)

                        _todaySongIds.value = emptyList()
                        saveTodaySongIdsToPrefs()

                        _activeOverlayMessage.value = "📋 Lista del Día guardada: $dateLabel"
                        triggerHapticFeedback()
                        viewModelScope.launch {
                            delay(3500)
                            if (_activeOverlayMessage.value?.contains(dateLabel) == true) {
                                _activeOverlayMessage.value = null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing incoming message: ${e.message}")
            }
        }
    }

    // Export Songs JSON
    fun exportSongbook(context: Context, onShareFileReady: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSongs = songs.value
            val array = JSONArray()
            for ((index, song) in currentSongs.withIndex()) {
                val obj = JSONObject().apply {
                    put("title", song.title)
                    put("category", song.category)
                    put("key", song.key)
                    put("lyrics", song.lyrics)
                    put("notes", song.notes)
                    put("dateCreated", song.dateCreated)
                    put("displayOrder", if (song.displayOrder != 0) song.displayOrder else index)
                }
                array.put(obj)
            }
            
            val jsonString = array.toString(2)
            val cacheDir = context.cacheDir
            val exportFile = File(cacheDir, "cancionero.alabanza")
            FileOutputStream(exportFile).use { fos ->
                fos.write(jsonString.toByteArray())
            }
            withContext(Dispatchers.Main) {
                onShareFileReady(exportFile)
            }
        }
    }

    // Smart Import Songs
    private fun cleanAndExtractLyrics(rawLyrics: Any?): String {
        if (rawLyrics == null) return ""
        
        fun extractTextFromVerseObject(jsonObj: JSONObject): String? {
            val fields = listOf("full_text", "text", "lyrics", "content", "verse", "body")
            for (field in fields) {
                val value = jsonObj.optString(field, "")
                if (value.isNotEmpty()) {
                    return value
                }
            }
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val valStr = jsonObj.optString(key, "")
                if (valStr.isNotEmpty() && key != "number" && key != "id" && key != "type") {
                    return valStr
                }
            }
            return null
        }

        fun processJsonArray(array: JSONArray): String {
            val sb = StringBuilder()
            for (i in 0 until array.length()) {
                val item = array.get(i)
                if (item is JSONObject) {
                    val text = extractTextFromVerseObject(item)
                    if (!text.isNullOrEmpty()) {
                        if (sb.isNotEmpty()) {
                            sb.append("\n\n")
                        }
                        sb.append(text)
                    }
                } else if (item != null) {
                    val text = item.toString()
                    if (text.isNotEmpty()) {
                        if (sb.isNotEmpty()) {
                            sb.append("\n\n")
                        }
                        sb.append(text)
                    }
                }
            }
            return sb.toString()
        }

        fun processJsonObject(obj: JSONObject): String {
            val arrayFields = listOf("verses", "stanzas", "paragraphs", "lines")
            for (field in arrayFields) {
                val arr = obj.optJSONArray(field)
                if (arr != null) {
                    return processJsonArray(arr)
                }
            }
            val directText = extractTextFromVerseObject(obj)
            if (!directText.isNullOrEmpty()) {
                return directText
            }
            return obj.toString()
        }

        var cleanedLyrics = ""
        try {
            if (rawLyrics is JSONArray) {
                cleanedLyrics = processJsonArray(rawLyrics)
            } else if (rawLyrics is JSONObject) {
                cleanedLyrics = processJsonObject(rawLyrics)
            } else {
                val str = rawLyrics.toString().trim()
                if (str.startsWith("[") && str.endsWith("]")) {
                    try {
                        val array = JSONArray(str)
                        cleanedLyrics = processJsonArray(array)
                    } catch (e: Exception) {
                        cleanedLyrics = str
                    }
                } else if (str.startsWith("{") && str.endsWith("}")) {
                    try {
                        val obj = JSONObject(str)
                        cleanedLyrics = processJsonObject(obj)
                    } catch (e: Exception) {
                        cleanedLyrics = str
                    }
                } else {
                    cleanedLyrics = str
                }
            }
        } catch (e: Exception) {
            cleanedLyrics = rawLyrics.toString()
        }

        if (cleanedLyrics.contains("\\")) {
            cleanedLyrics = cleanedLyrics
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\t", "\t")
        }

        if (cleanedLyrics.startsWith("\"") && cleanedLyrics.endsWith("\"") && cleanedLyrics.length >= 2) {
            cleanedLyrics = cleanedLyrics.substring(1, cleanedLyrics.length - 1)
        }

        return cleanedLyrics.trim()
    }

    fun analyzeImportFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (jsonString.isEmpty()) return@launch

                val array = JSONArray(jsonString)
                val candidates = mutableListOf<Song>()
                var newCount = 0
                var updatedCount = 0
                var duplicateCount = 0

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val title = obj.getString("title")
                    val category = obj.optString("category", "Otros")
                    val key = obj.optString("key", "C")
                    
                    val lyricsRaw = obj.opt("lyrics")
                    val lyrics = cleanAndExtractLyrics(lyricsRaw)
                    
                    val notes = obj.optString("notes", "")
                    val dateCreated = obj.optLong("dateCreated", System.currentTimeMillis())
                    val displayOrder = obj.optInt("displayOrder", i)

                    val existingSong = songs.value.find { it.title.equals(title, ignoreCase = true) }

                    if (existingSong == null) {
                        newCount++
                        candidates.add(Song(title = title, category = category, key = key, lyrics = lyrics, notes = notes, dateCreated = dateCreated, displayOrder = displayOrder))
                    } else {
                        // Compare content to see if it is updated or duplicate
                        val isIdentical = existingSong.category == category &&
                                          existingSong.key == key &&
                                          existingSong.lyrics == lyrics &&
                                          existingSong.notes == notes &&
                                          existingSong.displayOrder == displayOrder
                        if (isIdentical) {
                            duplicateCount++
                        } else {
                            updatedCount++
                            candidates.add(existingSong.copy(category = category, key = key, lyrics = lyrics, notes = notes, displayOrder = displayOrder))
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    importCandidates = candidates
                    importStats = ImportStats(newCount, updatedCount, duplicateCount)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing import file: ${e.message}")
            }
        }
    }

    fun confirmImport(overwriteExisting: Boolean, onComplete: () -> Unit) {
        val candidates = importCandidates ?: return
        viewModelScope.launch(Dispatchers.IO) {
            for (song in candidates) {
                val existingSong = songs.value.find { it.title.equals(song.title, ignoreCase = true) }
                if (existingSong == null) {
                    repository.insertSong(song)
                } else if (overwriteExisting) {
                    // Overwrite details but preserve favorite status!
                    repository.updateSong(song.copy(id = existingSong.id, isFavorite = existingSong.isFavorite, displayOrder = song.displayOrder))
                }
            }
            withContext(Dispatchers.Main) {
                songBookSortMode = "Manual"
                clearImportState()
                onComplete()
            }
        }
    }

    fun reorderSongs(fromIndex: Int, toIndex: Int, currentFilteredList: List<Song>) {
        if (fromIndex !in currentFilteredList.indices || toIndex !in currentFilteredList.indices) return
        val fullList = songs.value.toMutableList()
        val movedSong = currentFilteredList[fromIndex]
        val targetSong = currentFilteredList[toIndex]

        val idx1 = fullList.indexOfFirst { it.id == movedSong.id }
        val idx2 = fullList.indexOfFirst { it.id == targetSong.id }

        if (idx1 != -1 && idx2 != -1) {
            val item = fullList.removeAt(idx1)
            fullList.add(idx2, item)
            val updatedSongs = fullList.mapIndexed { index, song ->
                song.copy(displayOrder = index)
            }
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateSongs(updatedSongs)
            }
        }
    }

    fun clearImportState() {
        importCandidates = null
        importStats = null
    }

    fun addCommand(text: String) {
        viewModelScope.launch {
            val currentCommands = commands.value
            val nextOrder = (currentCommands.maxOfOrNull { it.displayOrder } ?: -1) + 1
            repository.insertCommand(WorshipCommand(text = text, displayOrder = nextOrder))
        }
    }

    fun updateCommand(command: WorshipCommand) {
        viewModelScope.launch {
            repository.updateCommand(command)
        }
    }

    fun deleteCommand(command: WorshipCommand) {
        viewModelScope.launch {
            repository.deleteCommand(command)
            // Reorder remaining commands to maintain contiguous display orders
            val remaining = commands.value.filter { it.id != command.id }
            remaining.forEachIndexed { index, cmd ->
                repository.updateCommand(cmd.copy(displayOrder = index))
            }
        }
    }

    fun moveCommandUp(command: WorshipCommand) {
        viewModelScope.launch {
            val currentList = commands.value
            val index = currentList.indexOfFirst { it.id == command.id }
            if (index > 0) {
                val other = currentList[index - 1]
                repository.updateCommand(command.copy(displayOrder = other.displayOrder))
                repository.updateCommand(other.copy(displayOrder = command.displayOrder))
            }
        }
    }

    fun moveCommandDown(command: WorshipCommand) {
        viewModelScope.launch {
            val currentList = commands.value
            val index = currentList.indexOfFirst { it.id == command.id }
            if (index != -1 && index < currentList.size - 1) {
                val other = currentList[index + 1]
                repository.updateCommand(command.copy(displayOrder = other.displayOrder))
                repository.updateCommand(other.copy(displayOrder = command.displayOrder))
            }
        }
    }

    fun resetCommandsToDefault() {
        viewModelScope.launch {
            repository.resetCommandsToDefault()
        }
    }

    fun backupDatabase(outputStream: java.io.OutputStream, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    // Checkpoint WAL first to ensure all changes are written to the main db file
                    val db = com.example.data.AppDatabase.getDatabase(getApplication())
                    db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

                    val dbFile = getApplication<Application>().getDatabasePath("cancionero_database")
                    if (dbFile.exists()) {
                        dbFile.inputStream().use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error backing up database", e)
                    false
                }
            }
            if (result) onSuccess() else onError("Error al respaldar la base de datos")
        }
    }

    fun exportSongbookJson(outputStream: java.io.OutputStream, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentSongs = songs.value
                val array = org.json.JSONArray()
                for (song in currentSongs) {
                    val obj = org.json.JSONObject().apply {
                        put("title", song.title)
                        put("category", song.category)
                        put("key", song.key)
                        put("lyrics", song.lyrics)
                        put("notes", song.notes)
                        put("dateCreated", song.dateCreated)
                    }
                    array.put(obj)
                }
                val jsonString = array.toString(2)
                outputStream.use { output ->
                    output.write(jsonString.toByteArray())
                }
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting songbook JSON", e)
                withContext(Dispatchers.Main) {
                    onError("Error al exportar canciones en formato JSON")
                }
            }
        }
    }

    fun restoreDatabase(inputStream: java.io.InputStream, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    // Close database connections completely
                    com.example.data.AppDatabase.closeDatabase()

                    val dbFile = getApplication<Application>().getDatabasePath("cancionero_database")
                    val walFile = java.io.File(dbFile.path + "-wal")
                    val shmFile = java.io.File(dbFile.path + "-shm")

                    if (dbFile.exists()) dbFile.delete()
                    if (walFile.exists()) walFile.delete()
                    if (shmFile.exists()) shmFile.delete()

                    dbFile.parentFile?.mkdirs()

                    inputStream.use { input ->
                        dbFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring database", e)
                    false
                }
            }
            if (result) onSuccess() else onError("Error al restaurar la base de datos")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopWorshipSession()
        disconnectFromLeader()
    }
}
