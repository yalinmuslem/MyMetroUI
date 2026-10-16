package com.mymetroui.launcher.ui.start

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.R
import com.mymetroui.launcher.data.live.LiveFace
import com.mymetroui.launcher.data.live.LiveKind
import com.mymetroui.launcher.domain.TileSize
import com.mymetroui.launcher.ui.TileUi
import com.mymetroui.launcher.ui.motion.FlipSchedule
import com.mymetroui.launcher.ui.motion.MotionSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Stateless live tile: front is the normal tile, back shows [face]; [flipped] drives the rotation. */
@Composable
fun LiveTileView(
    ui: TileUi,
    kind: LiveKind,
    face: LiveFace?,
    icon: Drawable,
    flipped: Boolean,
    modifier: Modifier = Modifier,
) {
    val id = ui.tile.id
    val canFlip = ui.tile.size != TileSize.SMALL && face != null && face !is LiveFace.NeedsPermission
    val rotation by animateFloatAsState(
        if (flipped && canFlip) 180f else 0f, tween(MotionSpec.FlipDurationMs), label = "flip",
    )
    Box(modifier.graphicsLayer { rotationX = rotation; cameraDistance = 12f * density }.clipToBounds()) {
        if (rotation <= 90f) {
            TileView(ui, icon, Modifier.fillMaxSize())
            if (face is LiveFace.NeedsPermission && ui.tile.size != TileSize.SMALL) {
                Text(
                    stringResource(R.string.live_tap_to_allow),
                    Modifier.align(Alignment.TopStart).padding(8.dp).testTag("live-permission-$id"),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Box(
                Modifier.fillMaxSize().graphicsLayer { rotationX = 180f } // undo the mirroring of the back side
                    .background(MaterialTheme.colorScheme.primary).testTag("live-back-$id"),
            ) { BackFace(face) }
        }
    }
}

@Composable
private fun BoxScope.BackFace(face: LiveFace?) {
    when (face) {
        is LiveFace.Text -> Column(Modifier.padding(8.dp).align(Alignment.Center)) {
            Text(face.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            face.lines.forEach { Text(it, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        is LiveFace.Photo -> {
            val context = LocalContext.current
            val bitmap by produceState<ImageBitmap?>(null, face.uri) {
                value = withContext(Dispatchers.IO) { loadPhoto(context, face.uri) }
            }
            bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        else -> Unit
    }
}

/** Decodes a downsampled copy; any failure (deleted file, revoked access) yields null. */
private fun loadPhoto(context: Context, uri: String, maxPx: Int = 600): ImageBitmap? = try {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
    resolver.openInputStream(Uri.parse(uri))?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }?.asImageBitmap()
} catch (_: Exception) {
    null
}

/**
 * Stateful live tile: collects the face and runs the flip loop while [flipEnabled].
 * When the loop is cancelled (edit mode, tile resized/removed, Start hidden) the tile returns to the front.
 */
@Composable
fun LiveTile(
    ui: TileUi,
    kind: LiveKind,
    icon: Drawable,
    liveFace: (LiveKind) -> Flow<LiveFace?>,
    flipEnabled: Boolean,
    onFlipped: (LiveKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val face by produceState<LiveFace?>(null, kind) { liveFace(kind).collect { value = it } }
    var flipped by remember { mutableStateOf(false) }
    val tile = ui.tile

    LaunchedEffect(flipEnabled, tile.id, tile.size) {
        flipped = false
        val first = FlipSchedule.nextFlipAt(tile, System.currentTimeMillis())
        if (!flipEnabled || first == null) return@LaunchedEffect
        delay(first - System.currentTimeMillis())
        val interval = MotionSpec.FlipIntervalMs.toLong()
        val active = 2L * MotionSpec.FlipDurationMs + MotionSpec.FlipHoldMs
        while (true) {
            if (face != null) {
                flipped = true
                delay(MotionSpec.FlipDurationMs + MotionSpec.FlipHoldMs.toLong())
                flipped = false
                delay(MotionSpec.FlipDurationMs.toLong())
                onFlipped(kind)
                delay((interval - active).coerceAtLeast(0))
            } else {
                delay(interval)
            }
        }
    }
    // gate on flipEnabled directly so the tile returns to the front at once, without waiting for the effect to restart
    LiveTileView(ui, kind, face, icon, flipped && flipEnabled, modifier)
}
