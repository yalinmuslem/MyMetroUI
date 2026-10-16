package com.mymetroui.launcher.ui.hubs

import java.io.InputStream
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Largest power of two that keeps both sides at or above [target]; never below 1. */
fun sampleSize(width: Int, height: Int, target: Int): Int {
    var s = 1
    while (width / (s * 2) >= target && height / (s * 2) >= target) s *= 2
    return s
}

/** Decodes a downsampled copy of [uri]; any failure (missing file, revoked grant, bad format) gives null. */
suspend fun decodeThumbnail(open: (String) -> InputStream?, uri: String, target: Int = 256): ImageBitmap? =
    withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return@withContext null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, target) }
            open(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
