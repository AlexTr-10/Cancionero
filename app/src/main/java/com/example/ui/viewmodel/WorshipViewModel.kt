package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.data.model.AnnualScheduleItem
import com.example.data.model.BulletinDay
import com.example.data.model.DocumentEntity
import com.example.data.model.MeetingCommitment
import com.example.data.model.MeetingMinute
import com.example.data.model.WeeklyBulletin
import com.example.data.repository.ActaRepository
import com.example.data.repository.BackupRepository
import com.example.data.repository.BackupSummaryStats
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class Screen {
    Boletin,
    Cronograma,
    ModoPulpito,
    ActasReunion,
    Documentos
}

class WorshipViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("church_prefs", Context.MODE_PRIVATE)
    private val actaRepository = ActaRepository(application)
    private val backupRepository = BackupRepository(application)
    private val documentRepository = DocumentRepository(application)

    // Documentos Flow
    val documents: StateFlow<List<DocumentEntity>> = documentRepository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation
    private val _currentScreen = MutableStateFlow(Screen.Boletin)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Weekly Bulletin State
    private val _bulletin = MutableStateFlow(WeeklyBulletin.default())
    val bulletin: StateFlow<WeeklyBulletin> = _bulletin.asStateFlow()

    // Weekly Bulletin History State
    private val _bulletinHistory = MutableStateFlow<List<WeeklyBulletin>>(emptyList())
    val bulletinHistory: StateFlow<List<WeeklyBulletin>> = _bulletinHistory.asStateFlow()

    // Committees State
    private val _committees = MutableStateFlow<List<String>>(emptyList())
    val committees: StateFlow<List<String>> = _committees.asStateFlow()

    // Annual Schedule State
    private val _scheduleList = MutableStateFlow<List<AnnualScheduleItem>>(emptyList())
    val scheduleList: StateFlow<List<AnnualScheduleItem>> = _scheduleList.asStateFlow()

    // Meeting Minutes State
    private val _meetingMinutes = MutableStateFlow<List<MeetingMinute>>(emptyList())
    val meetingMinutes: StateFlow<List<MeetingMinute>> = _meetingMinutes.asStateFlow()

    // Dark Mode for normal view (optional)
    var isDarkMode by mutableStateOf(sharedPrefs.getBoolean("dark_mode", false))
        private set

    init {
        loadData()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        sharedPrefs.edit().putBoolean("dark_mode", isDarkMode).apply()
    }

    // --- Backup & Restore ---
    fun exportBackup(onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = backupRepository.exportBackupJson()
                onSuccess(file)
            } catch (e: Exception) {
                onError(e.message ?: "Error desconocido al exportar el respaldo.")
            }
        }
    }

    fun shareBackupFile(file: File) {
        backupRepository.shareBackupFile(file)
    }

    fun importBackup(uri: Uri, onSuccess: (BackupSummaryStats) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val stats = backupRepository.importBackupFromUri(uri)
                loadData()
                onSuccess(stats)
            } catch (e: Exception) {
                onError(e.message ?: "Error al importar la copia de seguridad. Verifica que el archivo sea válido.")
            }
        }
    }

    fun reloadData() {
        loadData()
    }

    // --- Save and Load logic ---
    private fun loadData() {
        // Load Committees
        loadCommittees()

        // Load Weekly Bulletin
        val bulletinJson = sharedPrefs.getString("weekly_bulletin", "") ?: ""
        _bulletin.value = deserializeBulletin(bulletinJson)

        // Load Bulletin History
        val historyJson = sharedPrefs.getString("bulletin_history", "") ?: ""
        _bulletinHistory.value = deserializeBulletinHistory(historyJson)

        // Load Annual Schedule
        val scheduleJson = sharedPrefs.getString("annual_schedule", "") ?: ""
        _scheduleList.value = deserializeSchedule(scheduleJson)

        // Load Meeting Minutes
        loadMeetingMinutes()
    }

    private fun saveBulletin(bulletin: WeeklyBulletin) {
        _bulletin.value = bulletin
        val jsonStr = serializeBulletin(bulletin)
        sharedPrefs.edit().putString("weekly_bulletin", jsonStr).apply()

        // Also automatically update this bulletin in history if it exists there to keep them in sync
        val currentHistory = _bulletinHistory.value
        if (currentHistory.any { it.id == bulletin.id }) {
            val updatedHistory = currentHistory.map {
                if (it.id == bulletin.id) bulletin else it
            }
            _bulletinHistory.value = updatedHistory
            saveHistory(updatedHistory)
        }
    }

    private fun saveHistory(list: List<WeeklyBulletin>) {
        val jsonStr = serializeBulletinHistory(list)
        sharedPrefs.edit().putString("bulletin_history", jsonStr).apply()
    }

    private fun saveSchedule(list: List<AnnualScheduleItem>) {
        _scheduleList.value = list
        val jsonStr = serializeSchedule(list)
        sharedPrefs.edit().putString("annual_schedule", jsonStr).apply()
    }

    // --- Bulletin updates ---
    fun updateDateRange(dateRange: String) {
        val updated = _bulletin.value.copy(dateRange = dateRange)
        saveBulletin(updated)
    }

    fun updateCommittee(committee: String) {
        val updated = _bulletin.value.copy(committee = committee)
        saveBulletin(updated)
    }

    fun updateGeneralAnnouncements(announcements: String) {
        val updated = _bulletin.value.copy(generalAnnouncements = announcements)
        saveBulletin(updated)
    }

    fun updateBulletinAttachedImagePath(path: String) {
        val updated = _bulletin.value.copy(attachedImagePath = path)
        saveBulletin(updated)
    }

    fun updateDayField(dayIndex: Int, field: String, value: String) {
        val currentDays = _bulletin.value.days.toMutableList()
        if (dayIndex in currentDays.indices) {
            val day = currentDays[dayIndex]
            val updatedDay = when (field) {
                "serviceName" -> day.copy(serviceName = value)
                "time" -> day.copy(time = value)
                "ushers" -> day.copy(ushers = value)
                "worshipTeam" -> day.copy(worshipTeam = value)
                "decom" -> day.copy(decom = value)
                "sound" -> day.copy(sound = value)
                "notesUniform" -> day.copy(notesUniform = value)
                else -> day
            }
            currentDays[dayIndex] = updatedDay
            val updated = _bulletin.value.copy(days = currentDays)
            saveBulletin(updated)
        }
    }

    // --- Historical Bulletin operations ---
    fun saveCurrentToHistory() {
        val current = _bulletin.value
        val bulletinToSave = if (current.id.isBlank()) {
            current.copy(id = UUID.randomUUID().toString())
        } else {
            current
        }
        _bulletin.value = bulletinToSave

        val currentHistory = _bulletinHistory.value.toMutableList()
        val index = currentHistory.indexOfFirst { it.id == bulletinToSave.id }
        if (index != -1) {
            currentHistory[index] = bulletinToSave
        } else {
            currentHistory.add(bulletinToSave)
        }

        _bulletinHistory.value = currentHistory
        saveHistory(currentHistory)

        // Save current bulletin too
        val jsonStr = serializeBulletin(bulletinToSave)
        sharedPrefs.edit().putString("weekly_bulletin", jsonStr).apply()
    }

    fun createNewBulletin() {
        val newB = WeeklyBulletin.default()
        _bulletin.value = newB
        val jsonStr = serializeBulletin(newB)
        sharedPrefs.edit().putString("weekly_bulletin", jsonStr).apply()
    }

    fun loadBulletinFromHistory(b: WeeklyBulletin) {
        _bulletin.value = b
        val jsonStr = serializeBulletin(b)
        sharedPrefs.edit().putString("weekly_bulletin", jsonStr).apply()
    }

    fun deleteBulletinFromHistory(id: String) {
        val updated = _bulletinHistory.value.filter { it.id != id }
        _bulletinHistory.value = updated
        saveHistory(updated)

        // If we deleted the active bulletin, load the first remaining one or create new
        if (_bulletin.value.id == id) {
            if (updated.isNotEmpty()) {
                loadBulletinFromHistory(updated.first())
            } else {
                createNewBulletin()
            }
        }
    }

    // --- Committees operations ---
    private fun loadCommittees() {
        val commJson = sharedPrefs.getString("custom_committees", "") ?: ""
        if (commJson.isEmpty()) {
            val defaultList = listOf("Jóvenes", "Damas", "Caballeros", "Escuela Dominical")
            _committees.value = defaultList
            saveCommittees(defaultList)
        } else {
            try {
                val array = JSONArray(commJson)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                _committees.value = list
            } catch (e: Exception) {
                _committees.value = listOf("Jóvenes", "Damas", "Caballeros", "Escuela Dominical")
            }
        }
    }

    private fun saveCommittees(list: List<String>) {
        val array = JSONArray()
        for (c in list) {
            array.put(c)
        }
        sharedPrefs.edit().putString("custom_committees", array.toString()).apply()
    }

    fun addCommittee(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !_committees.value.contains(trimmed)) {
            val updated = _committees.value + trimmed
            _committees.value = updated
            saveCommittees(updated)
        }
    }

    fun deleteCommittee(name: String) {
        val updated = _committees.value.filter { it != name }
        _committees.value = updated
        saveCommittees(updated)
    }

    // --- Annual Schedule operations ---
    fun addScheduleItem(date: String, committee: String, description: String, month: String) {
        val newItem = AnnualScheduleItem(
            id = UUID.randomUUID().toString(),
            date = date,
            committee = committee,
            description = description,
            month = month
        )
        val updatedList = _scheduleList.value + newItem
        saveSchedule(updatedList)
    }

    fun updateScheduleItem(id: String, date: String, committee: String, description: String, month: String) {
        val updatedList = _scheduleList.value.map {
            if (it.id == id) {
                it.copy(date = date, committee = committee, description = description, month = month)
            } else {
                it
            }
        }
        saveSchedule(updatedList)
    }

    fun deleteScheduleItem(id: String) {
        val updatedList = _scheduleList.value.filter { it.id != id }
        saveSchedule(updatedList)
    }

    // --- Backup & Restore operations ---
    fun exportBackup(): String {
        val obj = JSONObject().apply {
            put("version", 1)

            // Committees
            val commArray = JSONArray()
            for (c in _committees.value) {
                commArray.put(c)
            }
            put("committees", commArray)

            // Current Bulletin
            put("current_bulletin", JSONObject(serializeBulletin(_bulletin.value)))

            // Bulletin History
            val historyArray = JSONArray()
            for (b in _bulletinHistory.value) {
                historyArray.put(JSONObject(serializeBulletin(b)))
            }
            put("bulletin_history", historyArray)

            // Annual Schedule
            put("annual_schedule", JSONArray(serializeSchedule(_scheduleList.value)))
        }
        return obj.toString(2)
    }

    fun importBackup(jsonStr: String): Boolean {
        if (jsonStr.isBlank()) return false
        try {
            val obj = JSONObject(jsonStr)

            // Validate version or key elements to verify it is a valid backup
            if (!obj.has("annual_schedule") && !obj.has("bulletin_history") && !obj.has("current_bulletin")) {
                return false
            }

            // 1. Committees
            if (obj.has("committees")) {
                val commArray = obj.getJSONArray("committees")
                val commList = mutableListOf<String>()
                for (i in 0 until commArray.length()) {
                    commList.add(commArray.getString(i))
                }
                _committees.value = commList
                sharedPrefs.edit().putString("custom_committees", commArray.toString()).apply()
            }

            // 2. Current Bulletin
            if (obj.has("current_bulletin")) {
                val currentObj = obj.getJSONObject("current_bulletin")
                val b = deserializeBulletin(currentObj.toString())
                _bulletin.value = b
                sharedPrefs.edit().putString("weekly_bulletin", currentObj.toString()).apply()
            }

            // 3. Bulletin History
            if (obj.has("bulletin_history")) {
                val historyArray = obj.getJSONArray("bulletin_history")
                val hList = mutableListOf<WeeklyBulletin>()
                for (i in 0 until historyArray.length()) {
                    hList.add(deserializeBulletin(historyArray.getJSONObject(i).toString()))
                }
                _bulletinHistory.value = hList
                sharedPrefs.edit().putString("bulletin_history", historyArray.toString()).apply()
            }

            // 4. Annual Schedule
            if (obj.has("annual_schedule")) {
                val scheduleArray = obj.getJSONArray("annual_schedule")
                val sList = mutableListOf<AnnualScheduleItem>()
                for (i in 0 until scheduleArray.length()) {
                    val sObj = scheduleArray.getJSONObject(i)
                    sList.add(
                        AnnualScheduleItem(
                            id = sObj.optString("id", ""),
                            date = sObj.optString("date", ""),
                            committee = sObj.optString("committee", ""),
                            description = sObj.optString("description", ""),
                            month = sObj.optString("month", "")
                        )
                    )
                }
                _scheduleList.value = sList
                sharedPrefs.edit().putString("annual_schedule", scheduleArray.toString()).apply()
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // --- JSON Serialization Helpers ---
    private fun serializeBulletin(bulletin: WeeklyBulletin): String {
        val obj = JSONObject().apply {
            put("id", bulletin.id)
            put("dateRange", bulletin.dateRange)
            put("committee", bulletin.committee)
            put("generalAnnouncements", bulletin.generalAnnouncements)
            put("attachedImagePath", bulletin.attachedImagePath)

            val daysArray = JSONArray()
            for (day in bulletin.days) {
                val dayObj = JSONObject().apply {
                    put("dayName", day.dayName)
                    put("serviceName", day.serviceName)
                    put("time", day.time)
                    put("ushers", day.ushers)
                    put("worshipTeam", day.worshipTeam)
                    put("decom", day.decom)
                    put("sound", day.sound)
                    put("notesUniform", day.notesUniform)
                }
                daysArray.put(dayObj)
            }
            put("days", daysArray)
        }
        return obj.toString()
    }

    private fun deserializeBulletin(jsonStr: String): WeeklyBulletin {
        if (jsonStr.isEmpty()) return WeeklyBulletin.default()
        try {
            val obj = JSONObject(jsonStr)
            val id = obj.optString("id", "")
            val dateRange = obj.optString("dateRange", "")
            val committee = obj.optString("committee", "")
            val generalAnnouncements = obj.optString("generalAnnouncements", "")
            val attachedImagePath = obj.optString("attachedImagePath", "")

            val daysList = mutableListOf<BulletinDay>()
            val daysArray = obj.optJSONArray("days")
            if (daysArray != null) {
                for (i in 0 until daysArray.length()) {
                    val dayObj = daysArray.getJSONObject(i)
                    daysList.add(
                        BulletinDay(
                            dayName = dayObj.optString("dayName", ""),
                            serviceName = dayObj.optString("serviceName", ""),
                            time = dayObj.optString("time", ""),
                            ushers = dayObj.optString("ushers", ""),
                            worshipTeam = dayObj.optString("worshipTeam", ""),
                            decom = dayObj.optString("decom", ""),
                            sound = dayObj.optString("sound", ""),
                            notesUniform = dayObj.optString("notesUniform", "")
                        )
                    )
                }
            }

            val finalDays = if (daysList.size == 7) daysList else WeeklyBulletin.default().days
            val finalId = if (id.isEmpty()) UUID.randomUUID().toString() else id
            return WeeklyBulletin(finalId, dateRange, committee, finalDays, generalAnnouncements, attachedImagePath)
        } catch (e: Exception) {
            return WeeklyBulletin.default()
        }
    }

    private fun serializeBulletinHistory(list: List<WeeklyBulletin>): String {
        val array = JSONArray()
        for (b in list) {
            array.put(JSONObject(serializeBulletin(b)))
        }
        return array.toString()
    }

    private fun deserializeBulletinHistory(jsonStr: String): List<WeeklyBulletin> {
        if (jsonStr.isEmpty()) return emptyList()
        try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<WeeklyBulletin>()
            for (i in 0 until array.length()) {
                list.add(deserializeBulletin(array.getJSONObject(i).toString()))
            }
            return list
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun serializeSchedule(items: List<AnnualScheduleItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("date", item.date)
                put("committee", item.committee)
                put("description", item.description)
                put("month", item.month)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeSchedule(jsonStr: String): List<AnnualScheduleItem> {
        if (jsonStr.isEmpty()) return emptyList()
        try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<AnnualScheduleItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AnnualScheduleItem(
                        id = obj.optString("id", ""),
                        date = obj.optString("date", ""),
                        committee = obj.optString("committee", ""),
                        description = obj.optString("description", ""),
                        month = obj.optString("month", "")
                    )
                )
            }
            return list
        } catch (e: Exception) {
            return emptyList()
        }
    }

    // --- Meeting Minutes Operations (Room DB) ---
    private fun loadMeetingMinutes() {
        viewModelScope.launch {
            actaRepository.allActas.collect { roomActas ->
                if (roomActas.isEmpty()) {
                    val legacyJson = sharedPrefs.getString("meeting_minutes", "") ?: ""
                    val legacyList = deserializeMeetingMinutesList(legacyJson)
                    if (legacyList.isNotEmpty()) {
                        for (item in legacyList) {
                            actaRepository.insertOrUpdateActa(item)
                        }
                        sharedPrefs.edit().remove("meeting_minutes").apply()
                        return@collect
                    }
                }
                _meetingMinutes.value = roomActas
            }
        }
    }

    fun saveMeetingMinute(minute: MeetingMinute) {
        viewModelScope.launch {
            actaRepository.insertOrUpdateActa(minute)
        }
    }

    fun deleteMeetingMinute(id: String) {
        viewModelScope.launch {
            actaRepository.deleteActa(id)
        }
    }

    private fun serializeMeetingMinuteObj(minute: MeetingMinute): JSONObject {
        return JSONObject().apply {
            put("id", minute.id)
            put("folioNumber", minute.folioNumber)
            put("dateTime", minute.dateTime)
            put("location", minute.location)
            put("meetingType", minute.meetingType)
            put("attendees", minute.attendees)
            put("absentees", minute.absentees)
            put("agenda", minute.agenda)
            put("discussion", minute.discussion)
            put("secretary", minute.secretary)
            put("president", minute.president)
            put("attachedImagePath", minute.attachedImagePath)
            put("createdAt", minute.createdAt)

            val commArray = JSONArray()
            for (c in minute.commitments) {
                val cObj = JSONObject().apply {
                    put("id", c.id)
                    put("agreement", c.agreement)
                    put("responsible", c.responsible)
                    put("dueDate", c.dueDate)
                }
                commArray.put(cObj)
            }
            put("commitments", commArray)
        }
    }

    private fun deserializeMeetingMinuteObj(obj: JSONObject): MeetingMinute {
        val id = obj.optString("id", UUID.randomUUID().toString())
        val folioNumber = obj.optString("folioNumber", "")
        val dateTime = obj.optString("dateTime", "")
        val location = obj.optString("location", "")
        val meetingType = obj.optString("meetingType", "Ordinaria")
        val attendees = obj.optString("attendees", "")
        val absentees = obj.optString("absentees", "")
        val agenda = obj.optString("agenda", "")
        val discussion = obj.optString("discussion", "")
        val secretary = obj.optString("secretary", "")
        val president = obj.optString("president", "")
        val attachedImagePath = obj.optString("attachedImagePath", "")
        val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

        val commList = mutableListOf<MeetingCommitment>()
        val commArray = obj.optJSONArray("commitments")
        if (commArray != null) {
            for (i in 0 until commArray.length()) {
                val cObj = commArray.getJSONObject(i)
                commList.add(
                    MeetingCommitment(
                        id = cObj.optString("id", UUID.randomUUID().toString()),
                        agreement = cObj.optString("agreement", ""),
                        responsible = cObj.optString("responsible", ""),
                        dueDate = cObj.optString("dueDate", "")
                    )
                )
            }
        }
        return MeetingMinute(
            id = id,
            folioNumber = folioNumber,
            dateTime = dateTime,
            location = location,
            meetingType = meetingType,
            attendees = attendees,
            absentees = absentees,
            agenda = agenda,
            discussion = discussion,
            commitments = commList,
            secretary = secretary,
            president = president,
            attachedImagePath = attachedImagePath,
            createdAt = createdAt
        )
    }

    private fun serializeMeetingMinutesList(list: List<MeetingMinute>): String {
        val array = JSONArray()
        for (m in list) {
            array.put(serializeMeetingMinuteObj(m))
        }
        return array.toString()
    }

    private fun deserializeMeetingMinutesList(jsonStr: String): List<MeetingMinute> {
        if (jsonStr.isBlank()) return emptyList()
        try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<MeetingMinute>()
            for (i in 0 until array.length()) {
                list.add(deserializeMeetingMinuteObj(array.getJSONObject(i)))
            }
            return list
        } catch (e: Exception) {
            return emptyList()
        }
    }

    // Documentos Functions
    fun saveDocument(
        title: String,
        category: String,
        notes: String,
        tags: String,
        uri: Uri,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                documentRepository.saveDocumentFromUri(title, category, notes, tags, uri)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            documentRepository.deleteDocument(doc)
        }
    }

    fun openDocument(doc: DocumentEntity) {
        documentRepository.openDocumentFile(doc)
    }

    fun shareDocument(doc: DocumentEntity) {
        documentRepository.shareDocumentFile(doc)
    }
}
