package com.mymetroui.launcher.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.mymetroui.launcher.domain.Tile

fun turnstileDelayMs(tile: Tile): Int =
    tile.row * MotionSpec.TurnstileStaggerMs * 2 + tile.col * MotionSpec.TurnstileStaggerMs

private fun fraction(elapsedMs: Float, delayMs: Int, durationMs: Int): Float =
    FastOutSlowInEasing.transform(((elapsedMs - delayMs) / durationMs).coerceIn(0f, 1f))

/** Rotation around the left edge: 0 at rest, -TurnstileAngle once the tile has fully swung out. */
fun turnstileAngle(tile: Tile, elapsedMs: Float): Float =
    -MotionSpec.TurnstileAngle * fraction(elapsedMs, turnstileDelayMs(tile), MotionSpec.TurnstileDurationMs)

/** 0 = tile is below/hidden, 1 = settled; staggered like the turnstile. */
fun flyInProgress(tile: Tile, elapsedMs: Float): Float =
    fraction(elapsedMs, turnstileDelayMs(tile), MotionSpec.FlyInDurationMs)

/**
 * Drives the turnstile (tiles swing out on launch, back in on return) and the fly-in (Start opened from Home).
 * Both run on an elapsed-time clock so playing backwards naturally reverses the stagger order.
 */
class TurnstileState {
    private val turn = Animatable(0f)
    private val fly = Animatable(MotionSpec.FlyInDurationMs + 4000f)
    private var maxDelay by mutableIntStateOf(0)

    val turnElapsedMs: Float get() = turn.value
    val flyElapsedMs: Float get() = fly.value
    private val turnTotal get() = (maxDelay + MotionSpec.TurnstileDurationMs).toFloat()

    /** 0 at rest, 1 when every tile has swung out. */
    val progress: Float get() = (turn.value / turnTotal).coerceIn(0f, 1f)

    fun update(tiles: List<Tile>) { maxDelay = tiles.maxOfOrNull(::turnstileDelayMs) ?: 0 }

    suspend fun playOut() { turn.animateTo(turnTotal, tween((turnTotal).toInt(), easing = LinearEasing)) }
    suspend fun playIn() { if (turn.value > 0f) turn.animateTo(0f, tween(turn.value.toInt().coerceAtLeast(1), easing = LinearEasing)) }

    suspend fun flyIn() {
        fly.snapTo(0f)
        fly.animateTo((maxDelay + MotionSpec.FlyInDurationMs).toFloat(), tween(maxDelay + MotionSpec.FlyInDurationMs, easing = LinearEasing))
    }
}
