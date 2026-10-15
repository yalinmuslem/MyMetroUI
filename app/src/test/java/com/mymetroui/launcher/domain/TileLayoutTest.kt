package com.mymetroui.launcher.domain

import com.mymetroui.launcher.domain.TileSize.MEDIUM
import com.mymetroui.launcher.domain.TileSize.SMALL
import com.mymetroui.launcher.domain.TileSize.WIDE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TileLayoutTest {
    private fun t(id: Long, size: TileSize, col: Int, row: Int, key: String = "k$id") = Tile(id, key, size, col, row)
    private fun List<Tile>.byId(id: Long) = first { it.id == id }
    private fun List<Tile>.assertValid(): List<Tile> {
        assertTrue("invalid layout: $this", TileLayout.isValid(this))
        return this
    }

    @Test fun place_firstFreeSlot() {
        var l = TileLayout.place(emptyList(), t(1, SMALL, 3, 9)).assertValid()
        assertEquals(0 to 0, l.byId(1).let { it.col to it.row })
        l = TileLayout.place(l, t(2, SMALL, 0, 0)).assertValid()
        assertEquals(1 to 0, l.byId(2).let { it.col to it.row })
    }

    @Test fun place_wideGoesBelowFilledRows() {
        // two MEDIUMs fill rows 0-1 completely
        var l = TileLayout.place(emptyList(), t(1, MEDIUM, 0, 0))
        l = TileLayout.place(l, t(2, MEDIUM, 0, 0))
        l = TileLayout.place(l, t(3, WIDE, 0, 0)).assertValid()
        assertEquals(2, l.byId(3).row)
        assertEquals(0, l.byId(3).col)
    }

    @Test fun place_wideFitsBelowSingleSmallRow() {
        var l = emptyList<Tile>()
        for (i in 1L..4L) l = TileLayout.place(l, t(i, SMALL, 0, 0))
        l = TileLayout.place(l, t(5, WIDE, 0, 0)).assertValid()
        assertEquals(1, l.byId(5).row)
    }

    @Test fun move_pushesOverlappedDown() {
        val l = listOf(t(1, MEDIUM, 0, 0), t(2, MEDIUM, 2, 0))
        val r = TileLayout.move(l, 1, 2, 0).assertValid()
        assertEquals(2, r.byId(1).col)
        assertEquals(2, r.byId(2).row)
    }

    @Test fun move_clampsColumn() {
        val r1 = TileLayout.move(listOf(t(1, MEDIUM, 0, 0)), 1, 3, 0).assertValid()
        assertEquals(2, r1.byId(1).col)
        val r2 = TileLayout.move(listOf(t(1, WIDE, 0, 0)), 1, 2, 0).assertValid()
        assertEquals(0, r2.byId(1).col)
    }

    @Test fun move_negativeRowClampsToZero() {
        val r = TileLayout.move(listOf(t(1, SMALL, 0, 3)), 1, 0, -5).assertValid()
        assertEquals(0, r.byId(1).row)
    }

    @Test fun resize_growPushesNeighbor() {
        val l = listOf(t(1, SMALL, 0, 0), t(2, SMALL, 1, 0))
        val r = TileLayout.resize(l, 1, MEDIUM).assertValid()
        assertEquals(2, r.byId(2).row)
    }

    @Test fun resize_shrinkLeavesGap() {
        val l = listOf(t(1, MEDIUM, 0, 0), t(2, SMALL, 2, 0))
        val r = TileLayout.resize(l, 1, SMALL).assertValid()
        assertEquals(l.byId(2), r.byId(2))
    }

    @Test fun compact_closesEmptyRows() {
        val l = listOf(t(1, MEDIUM, 0, 0), t(2, SMALL, 0, 5))
        val r = TileLayout.compact(l).assertValid()
        assertEquals(0, r.byId(1).row)
        assertEquals(2, r.byId(2).row)
    }

    @Test fun sanitize_fixesCorruptData() {
        val l = listOf(t(1, WIDE, 9, -3, "a"), t(2, SMALL, 0, 0, "a"), t(3, SMALL, 0, 0, "b"))
        val r = TileLayout.sanitize(l).assertValid()
        assertEquals(setOf("a", "b"), r.map { it.key }.toSet())
        assertEquals(2, r.size)
    }

    @Test fun randomOperationsKeepInvariants() {
        val rnd = Random(42)
        var l = emptyList<Tile>()
        var nextId = 1L
        repeat(500) {
            val sizes = TileSize.values()
            l = when (rnd.nextInt(5)) {
                0, 1 -> TileLayout.place(l, t(nextId, sizes[rnd.nextInt(3)], 0, 0).also { nextId++ })
                2 -> if (l.isEmpty()) l else TileLayout.move(l, l.random(rnd).id, rnd.nextInt(-2, 8), rnd.nextInt(-2, 12))
                3 -> if (l.isEmpty()) l else TileLayout.resize(l, l.random(rnd).id, sizes[rnd.nextInt(3)])
                else -> TileLayout.compact(l)
            }
            assertTrue("invalid after step $it: $l", TileLayout.isValid(l))
        }
    }
}
