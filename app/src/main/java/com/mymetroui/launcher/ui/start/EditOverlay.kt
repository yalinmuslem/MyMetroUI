package com.mymetroui.launcher.ui.start

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ButtonSize = 32.dp

/** Unpin (top-right) and resize (bottom-right) buttons for the selected tile, placed in grid coordinates. */
@Composable
fun BoxScope.EditButtons(
    id: Long,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    onUnpin: () -> Unit,
    onResize: () -> Unit,
) {
    EditButton("unpin-$id", "×", x + width - ButtonSize - 2.dp, y + 2.dp, onUnpin)
    EditButton("resize-$id", "↔", x + width - ButtonSize - 2.dp, y + height - ButtonSize - 2.dp, onResize)
}

@Composable
private fun BoxScope.EditButton(tag: String, glyph: String, x: Dp, y: Dp, onClick: () -> Unit) {
    Box(
        Modifier.align(Alignment.TopStart).offset(x, y).size(ButtonSize)
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, color = Color.White) }
}
