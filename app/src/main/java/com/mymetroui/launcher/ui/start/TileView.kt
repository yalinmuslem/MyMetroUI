package com.mymetroui.launcher.ui.start

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.domain.TileSize
import com.mymetroui.launcher.ui.TileUi
import com.mymetroui.launcher.ui.common.DrawableImage

@Composable
fun TileView(ui: TileUi, icon: Drawable, modifier: Modifier = Modifier) {
    val small = ui.tile.size == TileSize.SMALL
    Box(modifier.background(MaterialTheme.colorScheme.primary)) {
        Box(Modifier.align(Alignment.Center)) { DrawableImage(icon, if (small) 28.dp else 48.dp) }
        if (!small) {
            Text(
                ui.app.label,
                Modifier.align(Alignment.BottomStart).padding(8.dp),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
