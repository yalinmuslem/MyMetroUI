package com.mymetroui.launcher.ui.start

import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.data.AppInfo
import com.mymetroui.launcher.domain.TileLayout
import com.mymetroui.launcher.ui.TileUi
import com.mymetroui.launcher.ui.motion.tilt

private val Gap = 4.dp

@Composable
fun StartGrid(
    tiles: List<TileUi>,
    editing: Boolean,
    onTileTap: (TileUi, Rect) -> Unit,
    onTileLongPress: (TileUi) -> Unit,
    loadIcon: (AppInfo) -> Drawable,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val cell = (maxWidth - Gap * (TileLayout.COLUMNS + 1)) / TileLayout.COLUMNS
        val rows = tiles.maxOfOrNull { it.tile.row + it.tile.size.h } ?: 0
        Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height((cell + Gap) * rows + Gap)) {
            tiles.forEach { ui ->
                key(ui.tile.id) {
                    val t = ui.tile
                    var bounds by remember { mutableStateOf(Rect()) }
                    TileView(
                        ui,
                        remember(ui.app.key) { loadIcon(ui.app) },
                        Modifier
                            .offset(Gap + (cell + Gap) * t.col, Gap + (cell + Gap) * t.row)
                            .size(cell * t.size.w + Gap * (t.size.w - 1), cell * t.size.h + Gap * (t.size.h - 1))
                            .testTag("tile-${t.id}")
                            .onGloballyPositioned {
                                val r = it.boundsInWindow().toAndroidRectF()
                                bounds = Rect(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
                            }
                            .tilt(onTap = { onTileTap(ui, bounds) }, onLongPress = { onTileLongPress(ui) }),
                    )
                }
            }
        }
    }
}
