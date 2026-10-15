package com.mymetroui.launcher.ui

import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize
import com.mymetroui.launcher.domain.next
import com.mymetroui.launcher.ui.start.StartGrid
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditModeTest {
    @get:Rule val rule = createComposeRule()

    private var tiles by mutableStateOf(listOf<TileUi>())
    private var editing by mutableStateOf(false)
    private val moves = mutableListOf<Triple<Long, Int, Int>>()
    private val sizes = mutableListOf<TileSize>()
    private val unpinned = mutableListOf<Long>()

    private fun ui(id: Long, size: TileSize, col: Int, row: Int) =
        TileUi(Tile(id, "k$id", size, col, row), fakeApp("App$id", "p.$id"))

    private fun show(initial: List<TileUi>) {
        tiles = initial
        rule.setContent {
            MyMetroUITheme {
                StartGrid(
                    tiles = tiles,
                    editing = editing,
                    onTileTap = { _, _ -> },
                    onTileLongPress = { editing = true },
                    loadIcon = { ColorDrawable(0) },
                    onMove = { id, c, r -> moves += Triple(id, c, r) },
                    onResize = { id ->
                        tiles = tiles.map { if (it.tile.id == id) it.copy(tile = it.tile.copy(size = it.tile.size.next())) else it }
                        sizes += tiles.first { it.tile.id == id }.tile.size
                    },
                    onUnpin = { id -> unpinned += id; tiles = tiles.filter { it.tile.id != id } },
                    onExitEdit = { editing = false },
                )
            }
        }
    }

    private fun enterEdit(id: Long) = rule.onNodeWithTag("tile-$id").performTouchInput { longClick() }

    @Test fun longPress_entersEditAndShowsButtons() {
        show(listOf(ui(1, TileSize.MEDIUM, 0, 0)))
        rule.onNodeWithTag("unpin-1").assertDoesNotExist()
        enterEdit(1)
        assertTrue(editing)
        rule.onNodeWithTag("unpin-1").assertIsDisplayed()
        rule.onNodeWithTag("resize-1").assertIsDisplayed()
    }

    @Test fun resizeButton_cyclesSmallMediumWide() {
        show(listOf(ui(1, TileSize.SMALL, 0, 0)))
        enterEdit(1)
        val before = rule.onNodeWithTag("tile-1").getUnclippedBoundsInRoot().width.value
        repeat(3) { rule.onNodeWithTag("resize-1").performClick() }
        assertEquals(listOf(TileSize.MEDIUM, TileSize.WIDE, TileSize.SMALL), sizes)
        assertEquals(before, rule.onNodeWithTag("tile-1").getUnclippedBoundsInRoot().width.value, 0.5f)
    }

    @Test fun unpinButton_callsUnpin() {
        show(listOf(ui(1, TileSize.MEDIUM, 0, 0)))
        enterEdit(1)
        rule.onNodeWithTag("unpin-1").performClick()
        assertEquals(listOf(1L), unpinned)
    }

    @Test fun dragTile_callsMoveWithRoundedCell() {
        show(listOf(ui(1, TileSize.SMALL, 0, 0)))
        enterEdit(1)
        val step = with(rule.density) { (75.dp + 4.dp).toPx() } // cell + gap at 320dp width
        rule.onNodeWithTag("tile-1").performTouchInput {
            down(center)
            moveBy(Offset(1.2f * step, 0.9f * step))
            up()
        }
        assertEquals(listOf(Triple(1L, 1, 1)), moves)
    }

    @Test fun tapEmpty_exitsEdit() {
        show(listOf(ui(1, TileSize.SMALL, 0, 0)))
        enterEdit(1)
        rule.onNodeWithTag("start-grid").performTouchInput { click(Offset(width * 0.9f, height * 0.9f)) }
        assertFalse(editing)
    }

    @Test fun tileDisappearsWhileEditing_editStaysValid() {
        show(listOf(ui(1, TileSize.MEDIUM, 0, 0), ui(2, TileSize.MEDIUM, 2, 0)))
        enterEdit(1)
        rule.onNodeWithTag("unpin-1").assertIsDisplayed()
        rule.runOnIdle { tiles = tiles.filter { it.tile.id != 1L } }
        rule.onNodeWithTag("unpin-1").assertDoesNotExist()
        rule.onNodeWithTag("resize-1").assertDoesNotExist()
        rule.onNodeWithTag("tile-2").assertIsDisplayed()
    }
}
