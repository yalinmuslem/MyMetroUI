package com.mymetroui.launcher.ui

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.data.notif.NotificationController
import com.mymetroui.launcher.data.notif.NotificationRepository
import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize
import com.mymetroui.launcher.ui.home.HomeScreen
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ActionCenterHostTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val repo = NotificationRepository().also {
        it.onConnected(emptyList(), "me", object : NotificationController {
            override fun cancel(key: String) {}
            override fun cancelAll(keys: List<String>) {}
        })
    }
    private val app = fakeApp("Alpha", "p.alpha")

    private fun showHome(withTile: Boolean = false) {
        val tiles = FakeTileSource(if (withTile) listOf(Tile(1, app.key, TileSize.MEDIUM, 0, 0)) else emptyList())
        val apps = FakeAppSource(listOf(app))
        val start = StartViewModel(tiles, apps)
        val appList = AppListViewModel(tiles, apps)
        rule.setContent {
            MyMetroUITheme {
                HomeScreen(start, appList, theme = FakeThemeSource(), loadIcon = { ColorDrawable(0) }, controller = testController(repo))
            }
        }
    }

    private fun dragEdge(dy: Float) = rule.onNodeWithTag("action-center-edge").performTouchInput {
        down(center)
        moveBy(Offset(0f, dy))
        up()
    }

    @Test fun dragFromTopEdge_opensPanel() {
        showHome()
        dragEdge(400f)
        rule.waitForIdle()
        rule.onNodeWithTag("notif-empty").assertIsDisplayed()
    }

    @Test fun smallDrag_staysClosed() {
        showHome()
        dragEdge(15f)
        rule.waitForIdle()
        rule.onNodeWithTag("notif-empty").assertDoesNotExist()
    }

    @Test fun scrimTapCloses() {
        showHome()
        dragEdge(400f)
        rule.waitForIdle()
        rule.onNodeWithTag("action-center-scrim").performTouchInput { click(Offset(10f, height - 10f)) }
        rule.waitForIdle()
        rule.onNodeWithTag("notif-empty").assertDoesNotExist()
    }

    @Test fun panelIgnoredWhileEditing() {
        showHome(withTile = true)
        rule.onNodeWithTag("tile-1").performTouchInput { longClick() }
        rule.waitForIdle()
        // the edge strip is not even composed while editing, so the panel cannot be dragged open
        rule.onNodeWithTag("action-center-edge").assertDoesNotExist()
        rule.onNodeWithTag("notif-empty").assertDoesNotExist()
    }

    @Test fun backClosesPanelBeforeSettings() {
        showHome()
        rule.onNodeWithTag("to-app-list").performClick()
        rule.onNodeWithTag("settings-row").performClick()
        rule.onNodeWithTag("accent-red").assertIsDisplayed()
        dragEdge(400f)
        rule.waitForIdle()
        rule.onNodeWithTag("notif-empty").assertIsDisplayed()

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag("notif-empty").assertDoesNotExist()
        rule.onNodeWithTag("accent-red").assertIsDisplayed() // Settings still open

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag("accent-red").assertDoesNotExist()
    }

    @Test fun settingsButton_opensPanel() {
        showHome()
        rule.onNodeWithTag("to-app-list").performClick()
        rule.onNodeWithTag("settings-row").performClick()
        rule.onNodeWithTag("open-action-center").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("notif-empty").assertIsDisplayed()
        rule.onNodeWithTag("accent-red").assertDoesNotExist()
    }
}
