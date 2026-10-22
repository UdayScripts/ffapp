package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ChatImageHelper {

    private val bitmapCache = object : LruCache<Int, ImageBitmap>(30) {}

    /**
     * Reads an image from a content [Uri], downscales it to max 800px dimension,
     * compresses it as JPEG to stay well within Firestore document size limits,
     * and returns a Base64 string.
     */
    suspend fun encodeUriToBase64(context: Context, uri: Uri, maxDimension: Int = 800): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IllegalArgumentException("Unable to open selected image"))
                val rawBytes = inputStream.use { it.readBytes() }
                if (rawBytes.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("Selected image is empty"))
                }

                // Decode bounds first
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

                var sampleSize = 1
                val longestSide = max(boundsOptions.outWidth, boundsOptions.outHeight)
                if (longestSide > maxDimension) {
                    while ((longestSide / sampleSize) > maxDimension * 1.5) {
                        sampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                    ?: return@withContext Result.failure(IllegalArgumentException("Unsupported image format"))

                // Precise resize if still larger than maxDimension
                val width = decoded.width
                val height = decoded.height
                val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                    val ratio = maxDimension.toFloat() / max(width, height).toFloat()
                    val targetW = (width * ratio).roundToInt().coerceAtLeast(1)
                    val targetH = (height * ratio).roundToInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                } else {
                    decoded
                }

                // Compress iteratively to ensure compact payload (< 250KB raw bytes -> < 340KB Base64)
                val outputStream = ByteArrayOutputStream()
                var quality = 74
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                while (outputStream.size() > 240_000 && quality > 35) {
                    outputStream.reset()
                    quality -= 12
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                }

                val compressedBytes = outputStream.toByteArray()
                val base64Str = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                Result.success(base64Str)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Decodes a Base64-encoded image string into a Compose [ImageBitmap] with LRU caching.
     */
    fun decodeBase64ToImageBitmap(base64Str: String?): ImageBitmap? {
        if (base64Str.isNullOrBlank()) return null
        val key = base64Str.hashCode()
        bitmapCache.get(key)?.let { return it }

        return try {
            val cleanBase64 = if (base64Str.contains(",")) {
                base64Str.substringAfter(",")
            } else {
                base64Str
            }
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            if (bmp != null) {
                bitmapCache.put(key, bmp)
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }
}
