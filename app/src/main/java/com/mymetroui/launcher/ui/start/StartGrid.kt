package com.mymetroui.launcher.ui.start

import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.round
import com.mymetroui.launcher.data.AppInfo
import com.mymetroui.launcher.domain.TileLayout
import com.mymetroui.launcher.data.live.LiveFace
import com.mymetroui.launcher.data.live.LiveKind
import com.mymetroui.launcher.ui.TileContent
import com.mymetroui.launcher.ui.TileUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.mymetroui.launcher.ui.motion.MotionSpec
import com.mymetroui.launcher.ui.motion.TurnstileState
import com.mymetroui.launcher.ui.motion.flyInProgress
import com.mymetroui.launcher.ui.motion.tilt
import com.mymetroui.launcher.ui.motion.turnstileAngle
import androidx.compose.ui.graphics.TransformOrigin
import kotlin.math.roundToInt

private val Gap = 4.dp

@Composable
fun StartGrid(
    tiles: List<TileUi>,
    editing: Boolean,
    onTileTap: (TileUi, Rect) -> Unit,
    onTileLongPress: (TileUi) -> Unit,
    loadIcon: (AppInfo) -> Drawable,
    modifier: Modifier = Modifier,
    onMove: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onResize: (Long) -> Unit = {},
    onUnpin: (Long) -> Unit = {},
    onExitEdit: () -> Unit = {},
    turnstile: TurnstileState? = null,
    liveFace: (LiveKind) -> Flow<LiveFace?> = { emptyFlow() },
    flipEnabled: Boolean = true,
    onFlipped: (LiveKind) -> Unit = {},
) {
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(editing) { if (!editing) { selectedId = null; dragId = null } }
    LaunchedEffect(tiles) { turnstile?.update(tiles.map { it.tile }) }

    BoxWithConstraints(modifier) {
        val cell = (maxWidth - Gap * (TileLayout.COLUMNS + 1)) / TileLayout.COLUMNS
        val rows = tiles.maxOfOrNull { it.tile.row + it.tile.size.h } ?: 0
        val density = LocalDensity.current
        val stepPx = with(density) { (cell + Gap).toPx() }
        val selected = tiles.firstOrNull { it.tile.id == selectedId }

        Box(
            Modifier.fillMaxWidth().testTag("start-grid")
                .verticalScroll(rememberScrollState())
                .pointerInput(editing) { if (editing) detectTapGestures(onTap = { onExitEdit() }) }
                .height(max((cell + Gap) * rows + Gap, maxHeight)),
        ) {
            tiles.forEach { ui ->
                key(ui.tile.id) {
                    val t = ui.tile
                    val dragging = dragId == t.id
                    var bounds by remember { mutableStateOf(Rect()) }
                    val target = IntOffset(
                        (Gap + (cell + Gap) * t.col).let { with(density) { it.roundToPx() } },
                        (Gap + (cell + Gap) * t.row).let { with(density) { it.roundToPx() } },
                    )
                    val pos by animateIntOffsetAsState(target, MotionSpec.ReflowSpring, label = "reflow")
                    val scale by animateFloatAsState(
                        when { !editing -> 1f; dragging -> MotionSpec.EditLiftScale; else -> MotionSpec.EditScale }, label = "scale",
                    )
                    val alpha by animateFloatAsState(
                        if (editing && selectedId != null && selectedId != t.id) MotionSpec.EditDimAlpha else 1f, label = "alpha",
                    )

                    val gestures = if (editing) {
                        Modifier
                            .pointerInput(t.id) { detectTapGestures(onTap = { selectedId = t.id }) }
                            .pointerInput(t.id, t.col, t.row) {
                                detectDragGestures(
                                    onDragStart = { selectedId = t.id; dragId = t.id; dragOffset = Offset.Zero },
                                    onDrag = { change, delta -> change.consume(); dragOffset += delta },
                                    onDragEnd = {
                                        val col = t.col + (dragOffset.x / stepPx).roundToInt()
                                        val row = t.row + (dragOffset.y / stepPx).roundToInt()
                                        dragId = null; dragOffset = Offset.Zero
                                        if (col != t.col || row != t.row) onMove(t.id, col, row)
                                    },
                                    onDragCancel = { dragId = null; dragOffset = Offset.Zero },
                                )
                            }
                    } else {
                        Modifier.tilt(
                            onTap = { onTileTap(ui, bounds) },
                            onLongPress = { selectedId = t.id; onTileLongPress(ui) },
                        )
                    }

                    val icon = remember(ui.tile.key) { ui.app?.let(loadIcon) ?: android.graphics.drawable.ColorDrawable(0) }
                    val tileModifier = Modifier
                            .offset { if (dragging) pos + dragOffset.round() else pos }
                            .size(cell * t.size.w + Gap * (t.size.w - 1), cell * t.size.h + Gap * (t.size.h - 1))
                            .graphicsLayer {
                                scaleX = scale; scaleY = scale; this.alpha = alpha
                                turnstile?.let { ts ->
                                    val angle = turnstileAngle(t, ts.turnElapsedMs)
                                    if (angle != 0f) {
                                        rotationY = angle
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                        cameraDistance = 12f * this.density
                                    }
                                    val fly = flyInProgress(t, ts.flyElapsedMs)
                                    this.alpha = alpha * fly
                                    translationY = (1f - fly) * size.height * 0.5f
                                }
                            }
                            .testTag("tile-${t.id}")
                            .onGloballyPositioned {
                                val r = it.boundsInWindow().toAndroidRectF()
                                bounds = Rect(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
                            }
                            .then(gestures)
                    val liveKind = (ui.content as? TileContent.Live)?.kind
                    if (liveKind != null) {
                        LiveTile(ui, liveKind, icon, liveFace, flipEnabled && !editing, onFlipped, tileModifier)
                    } else {
                        TileView(ui, icon, tileModifier)
                    }
                }
            }
            if (editing && selected != null && dragId == null) {
                val t = selected.tile
                EditButtons(
                    id = t.id,
                    x = Gap + (cell + Gap) * t.col,
                    y = Gap + (cell + Gap) * t.row,
                    width = cell * t.size.w + Gap * (t.size.w - 1),
                    height = cell * t.size.h + Gap * (t.size.h - 1),
                    onUnpin = { onUnpin(t.id) },
                    onResize = { onResize(t.id) },
                )
            }
        }
    }
}
