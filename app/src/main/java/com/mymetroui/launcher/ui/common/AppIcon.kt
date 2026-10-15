package com.mymetroui.launcher.ui.common

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun DrawableImage(drawable: Drawable, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(drawable) {
        runCatching { drawable.toBitmap(128, 128).asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) Image(bitmap, contentDescription = null, modifier = modifier.size(size))
}
