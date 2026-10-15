package com.mymetroui.launcher.ui

import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize
import com.mymetroui.launcher.ui.start.StartGrid
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StartGridTest {
    @get:Rule val rule = createComposeRule()

    private fun ui(id: Long, label: String, size: TileSize, col: Int, row: Int) =
        TileUi(Tile(id, "k$id", size, col, row), fakeApp(label, "p.$id"))

    private fun show(
        tiles: List<TileUi>,
        onTap: (TileUi, Rect) -> Unit = { _, _ -> },
        onLong: (TileUi) -> Unit = {},
    ) = rule.setContent {
        MyMetroUITheme {
            StartGrid(tiles, editing = false, onTileTap = onTap, onTileLongPress = onLong, loadIcon = { ColorDrawable(0) })
        }
    }

    @Test fun tilesRenderedAtExpectedPositions() {
        show(listOf(ui(1, "Wide", TileSize.WIDE, 0, 0), ui(2, "Medium", TileSize.MEDIUM, 0, 2)))
        // Robolectric default width is 320dp: cell = (320 - 5*4) / 4 = 75dp
        val wide = rule.onNodeWithTag("tile-1").getUnclippedBoundsInRoot()
        assertEquals(4f, wide.left.value, 0.5f)
        assertEquals(4f, wide.top.value, 0.5f)
        assertEquals(312f, wide.width.value, 0.5f)
        assertEquals(154f, wide.height.value, 0.5f)
        val med = rule.onNodeWithTag("tile-2").getUnclippedBoundsInRoot()
        assertEquals(4f, med.left.value, 0.5f)
        assertEquals(162f, med.top.value, 0.5f)
        assertEquals(154f, med.width.value, 0.5f)
    }

    @Test fun smallTilesHideLabel() {
        show(listOf(ui(1, "Tiny", TileSize.SMALL, 0, 0), ui(2, "Mid", TileSize.MEDIUM, 2, 0)))
        rule.onNodeWithText("Tiny").assertDoesNotExist()
        rule.onNodeWithText("Mid").assertIsDisplayed()
    }

    @Test fun tapTile_callsCallbackWithBounds() {
        var tapped: TileUi? = null
        var rect: Rect? = null
        show(listOf(ui(1, "Mid", TileSize.MEDIUM, 0, 0)), onTap = { t, r -> tapped = t; rect = r })
        rule.onNodeWithTag("tile-1").performClick()
        assertEquals(1L, tapped?.tile?.id)
        assertNotNull(rect)
        assertTrue(rect!!.width() > 0)
    }

    @Test fun longPressTile_callsLongPress() {
        var pressed: TileUi? = null
        show(listOf(ui(1, "Mid", TileSize.MEDIUM, 0, 0)), onLong = { pressed = it })
        rule.onNodeWithTag("tile-1").performTouchInput { longClick() }
        assertEquals(1L, pressed?.tile?.id)
    }

    @Test fun contentScrollsWhenTilesExceedScreen() {
        show(listOf(ui(1, "Top", TileSize.MEDIUM, 0, 0), ui(2, "Far", TileSize.MEDIUM, 0, 40)))
        rule.onNodeWithTag("tile-2").assertIsNotDisplayed()
        rule.onNodeWithTag("tile-2").performScrollTo().assertIsDisplayed()
    }
}
