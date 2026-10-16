package com.mymetroui.launcher.ui

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.data.hub.HubKind
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HubHomeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val added = mutableListOf<HubKind>()

    private fun showHome(key: String = "hub:settings") {
        val tiles = FakeTileSource(listOf(Tile(1, key, TileSize.MEDIUM, 0, 0)))
        val apps = FakeAppSource(listOf(fakeApp("Alpha", "p.alpha")))
        rule.setContent {
            MyMetroUITheme {
                HomeScreen(
                    StartViewModel(tiles, apps), AppListViewModel(tiles, apps), theme = FakeThemeSource(),
                    loadIcon = { ColorDrawable(0) }, controller = testController(), onAddHub = { added += it },
                )
            }
        }
    }

    @Test fun tapHubTile_opensHub() {
        showHome()
        rule.onNodeWithTag("tile-1").performClick()
        rule.onNodeWithTag("hub-screen").assertIsDisplayed()
    }

    @Test fun back_closesHub() {
        showHome()
        rule.onNodeWithTag("tile-1").performClick()
        rule.waitForIdle()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag("hub-screen").assertDoesNotExist()
    }

    @Test fun editModeTap_doesNotOpen() {
        showHome()
        rule.onNodeWithTag("tile-1").performTouchInput { longClick() }
        rule.waitForIdle()
        rule.onNodeWithTag("tile-1").performClick()
        rule.onNodeWithTag("hub-screen").assertDoesNotExist()
    }

    @Test fun openSettingsButton_showsSettingsScreen() {
        showHome()
        rule.onNodeWithTag("tile-1").performClick()
        rule.onNodeWithTag("hub-open-settings").performClick()
        rule.onNodeWithTag("settings-close").assertIsDisplayed()
        rule.onNodeWithTag("hub-screen").assertDoesNotExist()
    }

    @Test fun settingsHubButton_callsOnAddHub() {
        showHome(key = "hub:photos")
        // open Settings through the app list row
        rule.onNodeWithTag("to-app-list").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("settings-row").performScrollTo().performClick()
        rule.onNodeWithTag("hub-add-people").performScrollTo().performClick()
        assertEquals(listOf(HubKind.PEOPLE), added)
    }
}
