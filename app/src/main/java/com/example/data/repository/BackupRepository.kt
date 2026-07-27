package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.model.ActaEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.Mosaic
import com.example.data.model.Song
import com.example.data.model.WorshipCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupSummaryStats(
    val actasCount: Int = 0,
    val documentsCount: Int = 0,
    val songsCount: Int = 0,
    val mosaicsCount: Int = 0,
    val commandsCount: Int = 0,
    val bulletinsCount: Int = 0,
    val scheduleCount: Int = 0
)

class BackupRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val sharedPrefs = context.getSharedPreferences("church_prefs", Context.MODE_PRIVATE)

    suspend fun exportBackupJson(): File = withContext(Dispatchers.IO) {
        val root = JSONObject()

        // 1. Metadata
        val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val dateReadable = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val meta = JSONObject().apply {
            put("app", "Secretaría Eclesiástica")
            put("version", "1.0")
            put("exportDate", dateReadable)
            put("timestamp", System.currentTimeMillis())
        }
        root.put("metadata", meta)

        // 2. Room Data: Actas
        val actas = database.actaDao().getActasSync()
        val actasArray = JSONArray()
        for (a in actas) {
            val aObj = JSONObject().apply {
                put("id", a.id)
                put("folioNumber", a.folioNumber)
                put("dateTime", a.dateTime)
                put("location", a.location)
                put("meetingType", a.meetingType)
                put("attendees", a.attendees)
                put("absentees", a.absentees)
                put("agenda", a.agenda)
                put("discussion", a.discussion)
                put("commitmentsJson", a.commitmentsJson)
                put("secretary", a.secretary)
                put("president", a.president)
                put("attachedImagePath", a.attachedImagePath)
                put("createdAt", a.createdAt)
            }
            actasArray.put(aObj)
        }
        root.put("actas", actasArray)

        // 3. Room Data: Songs
        val songs = database.songDao().getSongsSync()
        val songsArray = JSONArray()
        for (s in songs) {
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("category", s.category)
                put("key", s.key)
                put("lyrics", s.lyrics)
                put("notes", s.notes)
                put("dateCreated", s.dateCreated)
                put("isFavorite", s.isFavorite)
            }
            songsArray.put(sObj)
        }
        root.put("songs", songsArray)

        // 4. Room Data: Mosaics
        val mosaics = database.mosaicDao().getMosaicsSync()
        val mosaicsArray = JSONArray()
        for (m in mosaics) {
            val mObj = JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("songIds", m.songIds)
                put("dateCreated", m.dateCreated)
            }
            mosaicsArray.put(mObj)
        }
        root.put("mosaics", mosaicsArray)

        // 5. Room Data: Worship Commands (Púlpito)
        val commands = database.commandDao().getCommandsSync()
        val commandsArray = JSONArray()
        for (c in commands) {
            val cObj = JSONObject().apply {
                put("id", c.id)
                put("text", c.text)
                put("displayOrder", c.displayOrder)
            }
            commandsArray.put(cObj)
        }
        root.put("commands", commandsArray)

        // 6. Room Data: Documentos Importantes
        val documents = database.documentDao().getDocumentsSync()
        val documentsArray = JSONArray()
        for (d in documents) {
            val dObj = JSONObject().apply {
                put("id", d.id)
                put("title", d.title)
                put("category", d.category)
                put("fileUriOrPath", d.fileUriOrPath)
                put("fileName", d.fileName)
                put("mimeType", d.mimeType)
                put("fileSizeBytes", d.fileSizeBytes)
                put("notes", d.notes)
                put("tags", d.tags)
                put("dateAdded", d.dateAdded)
            }
            documentsArray.put(dObj)
        }
        root.put("documents", documentsArray)

        // 6. SharedPreferences Data
        val prefsObj = JSONObject().apply {
            put("weekly_bulletin", sharedPrefs.getString("weekly_bulletin", "") ?: "")
            put("bulletin_history", sharedPrefs.getString("bulletin_history", "") ?: "")
            put("annual_schedule", sharedPrefs.getString("annual_schedule", "") ?: "")
            put("committees", sharedPrefs.getString("committees", "") ?: "")
            put("dark_mode", sharedPrefs.getBoolean("dark_mode", false))
        }
        root.put("preferences", prefsObj)

        // Write to Cache file
        val fileName = "Backup_Secretaria_Eclesiastica_$timeStampStr.json"
        val cacheFile = File(context.cacheDir, fileName)
        cacheFile.writeText(root.toString(2))
        return@withContext cacheFile
    }

    fun shareBackupFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Copia de Seguridad - Secretaría Eclesiástica")
            putExtra(Intent.EXTRA_TEXT, "Adjunto respaldo completo de la aplicación Secretaría Eclesiástica.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Compartir Copia de Seguridad")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    suspend fun importBackupFromUri(uri: Uri): BackupSummaryStats = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("No se pudo abrir el archivo seleccionado.")
        
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.readText()
        reader.close()

        val root = JSONObject(jsonString)

        var actasCount = 0
        var songsCount = 0
        var mosaicsCount = 0
        var commandsCount = 0
        var bulletinsCount = 0
        var scheduleCount = 0

        // 1. Restore Actas
        if (root.has("actas")) {
            val array = root.getJSONArray("actas")
            val list = mutableListOf<ActaEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ActaEntity(
                        id = obj.optString("id"),
                        folioNumber = obj.optString("folioNumber"),
                        dateTime = obj.optString("dateTime"),
                        location = obj.optString("location"),
                        meetingType = obj.optString("meetingType", "Ordinaria"),
                        attendees = obj.optString("attendees"),
                        absentees = obj.optString("absentees"),
                        agenda = obj.optString("agenda"),
                        discussion = obj.optString("discussion"),
                        commitmentsJson = obj.optString("commitmentsJson", "[]"),
                        secretary = obj.optString("secretary"),
                        president = obj.optString("president"),
                        attachedImagePath = obj.optString("attachedImagePath", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            database.actaDao().deleteAllActas()
            if (list.isNotEmpty()) {
                database.actaDao().insertActas(list)
            }
            actasCount = list.size
        }

        // 2. Restore Songs
        if (root.has("songs")) {
            val array = root.getJSONArray("songs")
            val list = mutableListOf<Song>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Song(
                        id = obj.optLong("id", 0),
                        title = obj.optString("title"),
                        category = obj.optString("category"),
                        key = obj.optString("key"),
                        lyrics = obj.optString("lyrics"),
                        notes = obj.optString("notes"),
                        dateCreated = obj.optLong("dateCreated", System.currentTimeMillis()),
                        isFavorite = obj.optBoolean("isFavorite", false)
                    )
                )
            }
            database.songDao().deleteAllSongs()
            if (list.isNotEmpty()) {
                database.songDao().insertSongs(list)
            }
            songsCount = list.size
        }

        // 3. Restore Mosaics
        if (root.has("mosaics")) {
            val array = root.getJSONArray("mosaics")
            val list = mutableListOf<Mosaic>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Mosaic(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name"),
                        songIds = obj.optString("songIds"),
                        dateCreated = obj.optLong("dateCreated", System.currentTimeMillis())
                    )
                )
            }
            database.mosaicDao().deleteAllMosaics()
            if (list.isNotEmpty()) {
                database.mosaicDao().insertMosaics(list)
            }
            mosaicsCount = list.size
        }

        // 4. Restore Worship Commands
        if (root.has("commands")) {
            val array = root.getJSONArray("commands")
            val list = mutableListOf<WorshipCommand>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    WorshipCommand(
                        id = obj.optLong("id", 0),
                        text = obj.optString("text"),
                        displayOrder = obj.optInt("displayOrder", i)
                    )
                )
            }
            database.commandDao().deleteAllCommands()
            if (list.isNotEmpty()) {
                database.commandDao().insertCommands(list)
            }
            commandsCount = list.size
        }

        // 5. Restore Documentos
        var documentsCount = 0
        if (root.has("documents")) {
            val array = root.getJSONArray("documents")
            val list = mutableListOf<DocumentEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DocumentEntity(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        category = obj.optString("category"),
                        fileUriOrPath = obj.optString("fileUriOrPath"),
                        fileName = obj.optString("fileName"),
                        mimeType = obj.optString("mimeType"),
                        fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                        notes = obj.optString("notes"),
                        tags = obj.optString("tags"),
                        dateAdded = obj.optLong("dateAdded", System.currentTimeMillis())
                    )
                )
            }
            database.documentDao().deleteAllDocuments()
            if (list.isNotEmpty()) {
                database.documentDao().insertDocuments(list)
            }
            documentsCount = list.size
        }

        // 6. Restore Preferences
        if (root.has("preferences")) {
            val prefsObj = root.getJSONObject("preferences")
            val editor = sharedPrefs.edit()

            if (prefsObj.has("weekly_bulletin")) {
                editor.putString("weekly_bulletin", prefsObj.getString("weekly_bulletin"))
            }
            if (prefsObj.has("bulletin_history")) {
                val historyStr = prefsObj.getString("bulletin_history")
                editor.putString("bulletin_history", historyStr)
                if (historyStr.isNotBlank()) {
                    try {
                        bulletinsCount = JSONArray(historyStr).length()
                    } catch (_: Exception) {}
                }
            }
            if (prefsObj.has("annual_schedule")) {
                val schedStr = prefsObj.getString("annual_schedule")
                editor.putString("annual_schedule", schedStr)
                if (schedStr.isNotBlank()) {
                    try {
                        scheduleCount = JSONArray(schedStr).length()
                    } catch (_: Exception) {}
                }
            }
            if (prefsObj.has("committees")) {
                editor.putString("committees", prefsObj.getString("committees"))
            }
            if (prefsObj.has("dark_mode")) {
                editor.putBoolean("dark_mode", prefsObj.getBoolean("dark_mode"))
            }
            editor.apply()
        }

        return@withContext BackupSummaryStats(
            actasCount = actasCount,
            documentsCount = documentsCount,
            songsCount = songsCount,
            mosaicsCount = mosaicsCount,
            commandsCount = commandsCount,
            bulletinsCount = bulletinsCount,
            scheduleCount = scheduleCount
        )
    }
}
