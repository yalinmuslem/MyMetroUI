package com.mymetroui.launcher.ui

import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.data.live.LiveKind
import com.mymetroui.launcher.data.notif.badgeCounts
import com.mymetroui.launcher.data.notif.badgeKey
import com.mymetroui.launcher.data.notif.NotifItem
import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize
import com.mymetroui.launcher.ui.start.LiveTileView
import com.mymetroui.launcher.ui.start.StartGrid
import com.mymetroui.launcher.ui.start.TileView
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TileBadgeTest {
    @get:Rule val rule = createComposeRule()

    private fun appTile(size: TileSize = TileSize.MEDIUM) = TileUi(Tile(5, "k", size, 0, 0), fakeApp("App", "p.a"))

    private fun show(badge: Int, size: TileSize = TileSize.MEDIUM) = rule.setContent {
        MyMetroUITheme { TileView(appTile(size), ColorDrawable(0), Modifier.size(120.dp), badge = badge) }
    }

    @Test fun badgeShowsCount() {
        show(7)
        rule.onNodeWithTag("badge-5").assertIsDisplayed()
        rule.onNodeWithText("7").assertIsDisplayed()
    }

    @Test fun badgeCapsAt99Plus() {
        show(150)
        rule.onNodeWithText("99+").assertIsDisplayed()
    }

    @Test fun smallTileShowsNinePlus() {
        show(12, TileSize.SMALL)
        rule.onNodeWithText("9+").assertIsDisplayed()
    }

    @Test fun zeroCount_noBadge() {
        show(0)
        rule.onNodeWithTag("badge-5").assertDoesNotExist()
    }

    @Test fun liveTile_neverShowsBadge() {
        val live = TileUi(Tile(6, "live:clock", TileSize.MEDIUM, 0, 0), TileContent.Live(LiveKind.CLOCK))
        rule.setContent {
            MyMetroUITheme { TileView(live, ColorDrawable(0), Modifier.size(120.dp), badge = 5) }
        }
        rule.onNodeWithTag("badge-6").assertDoesNotExist()
    }

    @Test fun workProfileTile_usesItsOwnCount() {
        val personal = fakeApp("Chat", "p.same", userId = 0)
        val work = fakeApp("Chat work", "p.same", userId = 10)
        fun n(key: String, user: Int) = NotifItem(key, "p.same", user, "", "", 0, true, 1, null, false)
        val counts = badgeCounts(listOf(n("1", 0), n("2", 10), n("3", 10), n("4", 10)))
        rule.setContent {
            MyMetroUITheme {
                StartGrid(
                    tiles = listOf(
                        TileUi(Tile(1, personal.key, TileSize.MEDIUM, 0, 0), personal),
                        TileUi(Tile(2, work.key, TileSize.MEDIUM, 2, 0), work),
                    ),
                    editing = false, onTileTap = { _, _ -> }, onTileLongPress = {}, loadIcon = { ColorDrawable(0) },
                    badgeFor = { counts[badgeKey(it.component.packageName, it.userId)] ?: 0 },
                )
            }
        }
        rule.onNodeWithTag("badge-1").assertIsDisplayed()
        rule.onNodeWithText("1").assertIsDisplayed()
        rule.onNodeWithText("3").assertIsDisplayed()
    }
}
