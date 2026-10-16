package com.mymetroui.launcher.ui.start

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.data.hub.HubKind
import com.mymetroui.launcher.domain.TileSize
import com.mymetroui.launcher.ui.TileContent
import com.mymetroui.launcher.ui.TileUi
import com.mymetroui.launcher.ui.common.DrawableImage

@Composable
fun TileView(ui: TileUi, icon: Drawable, modifier: Modifier = Modifier, badge: Int = 0) {
    val small = ui.tile.size == TileSize.SMALL
    Box(modifier.background(MaterialTheme.colorScheme.primary)) {
        if (badge > 0 && ui.content is TileContent.App) {
            Text(
                badgeLabel(badge, small),
                Modifier.align(Alignment.TopEnd).padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("badge-${ui.tile.id}"),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
        val hub = (ui.content as? TileContent.Hub)?.kind
        Box(Modifier.align(Alignment.Center)) {
            if (hub != null) Text(hubGlyph(hub), style = MaterialTheme.typography.headlineLarge)
            else DrawableImage(icon, if (small) 28.dp else 48.dp)
        }
        if (!small) {
            Text(
                tileLabel(ui),
                Modifier.align(Alignment.BottomStart).padding(8.dp),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun tileLabel(ui: TileUi): String = when (val c = ui.content) {
    is TileContent.App -> c.app.label
    is TileContent.Live -> c.kind.name.lowercase().replaceFirstChar { it.uppercase() }
    is TileContent.Hub -> c.kind.name.lowercase().replaceFirstChar { it.uppercase() }
}

private fun badgeLabel(count: Int, small: Boolean): String = when {
    small -> if (count > 9) "9+" else count.toString()
    else -> if (count > 99) "99+" else count.toString()
}

private fun hubGlyph(kind: HubKind): String = when (kind) {
    HubKind.PEOPLE -> "P"
    HubKind.PHOTOS -> "G"
    HubKind.SETTINGS -> "S"
}
