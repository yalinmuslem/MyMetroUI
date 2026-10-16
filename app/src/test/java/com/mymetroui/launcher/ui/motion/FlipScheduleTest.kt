package com.mymetroui.launcher.ui.motion

import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipScheduleTest {
    private fun t(id: Long, size: TileSize = TileSize.MEDIUM) = Tile(id, "live:clock", size, 0, 0)

    @Test fun small_neverScheduled() {
        assertNull(FlipSchedule.nextFlipAt(t(1, TileSize.SMALL), 0))
    }

    @Test fun offsetsDifferPerTile() {
        val now = 1_000_000L
        val a = FlipSchedule.nextFlipAt(t(0), now)!!
        val b = FlipSchedule.nextFlipAt(t(1), now)!!
        assertEquals(MotionSpec.FlipStaggerMs.toLong(), ((b - a) % MotionSpec.FlipIntervalMs + MotionSpec.FlipIntervalMs) % MotionSpec.FlipIntervalMs)
    }

    @Test fun nextIsStrictlyAfterNow_andWithinInterval() {
        for (now in listOf(0L, 1L, 699L, 700L, 8_999L, 9_000L, 123_456_789L)) {
            for (id in 0L..9L) {
                val next = FlipSchedule.nextFlipAt(t(id), now)!!
                assertTrue("now=$now id=$id next=$next", next > now && next <= now + MotionSpec.FlipIntervalMs)
            }
        }
    }

    @Test fun sameIdSameOffset_modEight() {
        assertEquals(FlipSchedule.nextFlipAt(t(3), 5_000), FlipSchedule.nextFlipAt(t(11), 5_000))
    }
}
