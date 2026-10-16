package com.mymetroui.launcher.ui.motion

import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize

object FlipSchedule {
    /** First flip start strictly after [nowMs], staggered per tile; null for tiles that never flip. */
    fun nextFlipAt(tile: Tile, nowMs: Long): Long? {
        if (tile.size == TileSize.SMALL) return null
        val interval = MotionSpec.FlipIntervalMs.toLong()
        val offset = (tile.id.mod(8L)) * MotionSpec.FlipStaggerMs
        return (Math.floorDiv(nowMs - offset, interval) + 1) * interval + offset
    }
}
