package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.model.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class DocumentRepository(private val context: Context) {
    private val documentDao = AppDatabase.getDatabase(context).documentDao()

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    suspend fun saveDocumentFromUri(
        title: String,
        category: String,
        notes: String,
        tags: String,
        sourceUri: Uri
    ): DocumentEntity = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var mimeType = contentResolver.getType(sourceUri) ?: ""

        // Extract file name
        var fileName = "document_${System.currentTimeMillis()}"
        try {
            contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
        } catch (_: Exception) {}

        if (mimeType.isEmpty() || mimeType == "*/*") {
            mimeType = when {
                fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                fileName.endsWith(".doc", ignoreCase = true) || fileName.endsWith(".docx", ignoreCase = true) -> "application/msword"
                fileName.endsWith(".xls", ignoreCase = true) || fileName.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.ms-excel"
                fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
                else -> "application/octet-stream"
            }
        }

        // Copy file into app's internal "important_docs" folder
        val docsDir = File(context.filesDir, "important_docs")
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }

        val fileExtension = fileName.substringAfterLast('.', "")
        val uniqueName = "DOC_${UUID.randomUUID()}" + if (fileExtension.isNotEmpty()) ".$fileExtension" else ""
        val destFile = File(docsDir, uniqueName)

        var sizeBytes = 0L
        val inputStream: InputStream? = contentResolver.openInputStream(sourceUri)
        if (inputStream != null) {
            FileOutputStream(destFile).use { output ->
                sizeBytes = inputStream.copyTo(output)
            }
            inputStream.close()
        }

        val finalTitle = if (title.isNotBlank()) title else fileName.substringBeforeLast('.')

        val docEntity = DocumentEntity(
            id = UUID.randomUUID().toString(),
            title = finalTitle,
            category = category.ifBlank { "Otros" },
            fileUriOrPath = destFile.absolutePath,
            fileName = fileName,
            mimeType = mimeType,
            fileSizeBytes = sizeBytes,
            notes = notes,
            tags = tags,
            dateAdded = System.currentTimeMillis()
        )

        documentDao.insertDocument(docEntity)
        return@withContext docEntity
    }

    suspend fun deleteDocument(document: DocumentEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(document.fileUriOrPath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        documentDao.deleteDocument(document)
    }

    fun openDocumentFile(document: DocumentEntity) {
        try {
            val file = File(document.fileUriOrPath)
            val uri: Uri = if (file.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.parse(document.fileUriOrPath)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, document.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No hay visor instalado para este tipo de archivo.", Toast.LENGTH_LONG).show()
        }
    }

    fun shareDocumentFile(document: DocumentEntity) {
        try {
            val file = File(document.fileUriOrPath)
            val uri: Uri = if (file.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.parse(document.fileUriOrPath)
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = document.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, document.title)
                putExtra(Intent.EXTRA_TEXT, "Adjunto documento: ${document.title}\nCategoría: ${document.category}\n${document.notes}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Compartir Documento")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
