package com.mymetroui.launcher.ui

import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.mymetroui.launcher.data.live.LiveFace
import com.mymetroui.launcher.data.live.LiveKind
import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize
import com.mymetroui.launcher.ui.start.LiveTileView
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LiveTileViewTest {
    @get:Rule val rule = createComposeRule()

    private fun show(face: LiveFace?, flipped: Boolean, size: TileSize = TileSize.MEDIUM) {
        val ui = TileUi(Tile(7, "live:clock", size, 0, 0), TileContent.Live(LiveKind.CLOCK))
        rule.setContent {
            MyMetroUITheme {
                LiveTileView(ui, LiveKind.CLOCK, face, ColorDrawable(0), flipped, Modifier.size(150.dp))
            }
        }
        rule.waitForIdle()
    }

    private val text = LiveFace.Text("14:05", listOf("Thu, Oct 15"))

    @Test fun front_showsLabel() {
        show(text, flipped = false)
        rule.onNodeWithText("Clock").assertIsDisplayed()
        rule.onNodeWithTag("live-back-7").assertDoesNotExist()
    }

    @Test fun flipped_showsTextBack() {
        show(text, flipped = true)
        rule.onNodeWithTag("live-back-7").assertIsDisplayed()
        rule.onNodeWithText("14:05").assertIsDisplayed()
        rule.onNodeWithText("Thu, Oct 15").assertIsDisplayed()
    }

    @Test fun needsPermission_showsPrompt() {
        show(LiveFace.NeedsPermission, flipped = false)
        rule.onNodeWithTag("live-permission-7").assertIsDisplayed()
    }

    @Test fun nullFace_neverShowsBack() {
        show(null, flipped = true)
        rule.onNodeWithTag("live-back-7").assertDoesNotExist()
        rule.onNodeWithText("Clock").assertIsDisplayed()
    }

    @Test fun small_isNotLive() {
        show(text, flipped = true, size = TileSize.SMALL)
        rule.onNodeWithTag("live-back-7").assertDoesNotExist()
    }
}
