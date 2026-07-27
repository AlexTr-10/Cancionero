package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OcrScannerHelper {

    fun createTemporaryCameraUri(context: Context): Pair<Uri, File> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.filesDir, "scanned_docs")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val photoFile = File(storageDir, "DOC_$timeStamp.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        return Pair(uri, photoFile)
    }

    suspend fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.filesDir, "scanned_docs")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val destFile = File(storageDir, "DOC_$timeStamp.jpg")

        val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
        if (inputStream != null) {
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
        }
        return@withContext destFile.absolutePath
    }

    suspend fun processImageForText(context: Context, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            return@withContext suspendKtTask { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        continuation(Result.success(text))
                    }
                    .addOnFailureListener { e ->
                        continuation(Result.failure(e))
                    }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    private suspend inline fun <T> suspendKtTask(
        crossinline block: ((T) -> Unit) -> Unit
    ): T = kotlin.coroutines.suspendCoroutine { cont ->
        block { result ->
            cont.resumeWith(Result.success(result))
        }
    }
}
