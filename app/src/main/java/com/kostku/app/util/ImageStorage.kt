package com.kostku.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Menyalin foto pilihan pengguna ke penyimpanan internal aplikasi (diperkecil + diputar sesuai EXIF). */
object ImageStorage {
    suspend fun importImage(context: Context, uri: Uri, prefix: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2

                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: return@runCatching null

                val orientation = resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
                val degrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                val bitmap = if (degrees != 0f) {
                    val m = Matrix().apply { postRotate(degrees) }
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
                } else decoded

                val dir = File(context.filesDir, "photos").apply { mkdirs() }
                val out = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                out.absolutePath
            }.getOrNull()
        }
}
