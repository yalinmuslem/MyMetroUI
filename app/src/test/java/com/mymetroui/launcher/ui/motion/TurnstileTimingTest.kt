package com.mymetroui.launcher.ui.motion

import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnstileTimingTest {
    private fun t(col: Int, row: Int) = Tile(1, "k", TileSize.SMALL, col, row)
    private val s = MotionSpec.TurnstileStaggerMs

    @Test fun turnstileDelay_growsWithRowAndCol() {
        assertEquals(0, turnstileDelayMs(t(0, 0)))
        assertEquals(2 * s, turnstileDelayMs(t(2, 0)))
        assertEquals(4 * s, turnstileDelayMs(t(0, 2)))
    }

    @Test fun turnstileDelay_isMonotonicInRowAndInCol() {
        for (row in 0..5) for (col in 0..2) {
            assertTrue(turnstileDelayMs(t(col, row)) < turnstileDelayMs(t(col + 1, row)))
        }
        for (col in 0..3) for (row in 0..4) {
            assertTrue(turnstileDelayMs(t(col, row)) < turnstileDelayMs(t(col, row + 1)))
        }
    }

    @Test fun turnstileAngle_startsAtZeroEndsAtMinusAngle() {
        val tile = t(2, 1)
        val d = turnstileDelayMs(tile)
        assertEquals(0f, turnstileAngle(tile, 0f), 0f)
        assertEquals(0f, turnstileAngle(tile, d.toFloat()), 0f)
        assertEquals(-MotionSpec.TurnstileAngle, turnstileAngle(tile, (d + MotionSpec.TurnstileDurationMs).toFloat()), 0.001f)
        assertEquals(-MotionSpec.TurnstileAngle, turnstileAngle(tile, 99999f), 0.001f)
    }
}
