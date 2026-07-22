package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object PhotoStorageHelper {

    /**
     * Copies a source image Uri (from Gallery or Camera) to app's internal persistent storage.
     * Returns the file Uri string (file:///...) that will persist across reboots.
     */
    fun copyUriToInternalStorage(context: Context, sourceUri: Uri): String {
        return try {
            val directory = File(context.filesDir, "product_photos")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileName = "prod_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val targetFile = File(directory, fileName)

            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val outputStream = FileOutputStream(targetFile)

            if (inputStream != null) {
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(targetFile).toString()
            } else {
                sourceUri.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri.toString()
        }
    }

    /**
     * Creates a temporary file and FileProvider Uri for taking camera photos.
     */
    fun createCameraImageUri(context: Context): Pair<Uri, File>? {
        return try {
            val directory = File(context.cacheDir, "camera_photos")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileName = "camera_${System.currentTimeMillis()}.jpg"
            val file = File(directory, fileName)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Pair(uri, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes and scales a Bitmap from a Uri string (file://, content://, etc.)
     * for rendering on PDF or Canvas safely.
     */
    fun loadBitmapFromUri(context: Context, uriString: String, reqWidth: Int = 800, reqHeight: Int = 800): Bitmap? {
        if (uriString.isBlank()) return null
        return try {
            val uri = Uri.parse(uriString)

            // Decode dimensions first
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            var inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            // Decode bitmap with inSampleSize
            inputStream = context.contentResolver.openInputStream(uri)
            val decodedBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (decodedBitmap == null) return null

            // Correct EXIF orientation if needed
            rotateImageIfRequired(context, uri, decodedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateImageIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val ei = ExifInterface(inputStream)
            val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            inputStream.close()

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
