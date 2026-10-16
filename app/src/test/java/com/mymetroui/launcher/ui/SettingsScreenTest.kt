package com.mymetroui.launcher.ui

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.mymetroui.launcher.data.ThemeMode
import com.mymetroui.launcher.data.ThemeSettings
import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.ui.home.HomeScreen
import com.mymetroui.launcher.ui.settings.SettingsScreen
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

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule val rule = createComposeRule()

    private val accents = mutableListOf<String>()
    private val modes = mutableListOf<ThemeMode>()
    private var closed = 0

    private fun show(settings: ThemeSettings = ThemeSettings(accentId = "red")) = rule.setContent {
        MyMetroUITheme { SettingsScreen(settings, { accents += it }, { modes += it }, { closed++ }) }
    }

    @Test fun tapSwatch_callsOnAccent() {
        show()
        rule.onNodeWithTag("accent-cobalt").performClick()
        assertEquals(listOf("cobalt"), accents)
    }

    @Test fun toggleMode_callsOnMode() {
        show()
        rule.onNodeWithTag("mode-light").performClick()
        rule.onNodeWithTag("mode-dark").performClick()
        assertEquals(listOf(ThemeMode.LIGHT, ThemeMode.DARK), modes)
    }

    @Test fun selectedSwatch_isMarkedSelected() {
        show(ThemeSettings(accentId = "red"))
        rule.onNodeWithTag("accent-red").assertIsSelected()
        rule.onNodeWithTag("accent-cyan").assertIsNotSelected()
    }

    @Test fun closeButton_callsOnClose() {
        show()
        rule.onNodeWithTag("settings-close").performClick()
        assertEquals(1, closed)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsOverlayTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val theme = FakeThemeSource()

    private fun showHome() {
        val tiles = FakeTileSource()
        val apps = FakeAppSource(listOf(fakeApp("Alpha", "p.alpha")))
        val start = StartViewModel(tiles, apps)
        val appList = AppListViewModel(tiles, apps)
        rule.setContent {
            val s by theme.settings.collectAsState()
            MyMetroUITheme(s) { HomeScreen(start, appList, theme = theme, loadIcon = { ColorDrawable(0) }, controller = testController()) }
        }
        rule.onNodeWithTag("to-app-list").performClick()
        rule.onNodeWithTag("settings-row").performClick()
    }

    @Test fun settingsOverlay_backClosesSettingsFirst() {
        showHome()
        rule.onNodeWithTag("accent-red").assertExists()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag("accent-red").assertDoesNotExist()
        rule.onNodeWithTag("settings-row").assertExists() // still on the app list page
    }

    @Test fun changingAccent_updatesTheme() {
        showHome()
        rule.onNodeWithTag("accent-red").performClick()
        assertEquals("red", theme.settings.value.accentId)
        rule.onNodeWithTag("accent-red").assertIsSelected()
    }
}

@RunWith(RobolectricTestRunner::class)
class SettingsLiveSectionTest {
    @get:Rule val rule = createComposeRule()

    private val added = mutableListOf<com.mymetroui.launcher.data.live.LiveKind>()

    private fun show(pinned: Set<com.mymetroui.launcher.data.live.LiveKind> = emptySet()) = rule.setContent {
        MyMetroUITheme {
            SettingsScreen(ThemeSettings(), {}, {}, {}, pinnedLive = pinned, onAddLive = { added += it })
        }
    }

    @Test fun liveAddButtons_callOnAddLive() {
        show()
        rule.onNodeWithTag("live-add-clock").performScrollTo().performClick()
        rule.onNodeWithTag("live-add-calendar").performScrollTo().performClick()
        rule.onNodeWithTag("live-add-photos").performScrollTo().performClick()
        assertEquals(com.mymetroui.launcher.data.live.LiveKind.values().toList(), added)
    }

    @Test fun pinnedLiveButton_isDisabled() {
        show(setOf(com.mymetroui.launcher.data.live.LiveKind.CLOCK))
        rule.onNodeWithTag("live-add-clock").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("live-add-calendar").performScrollTo().assertIsEnabled()
    }
}

@RunWith(RobolectricTestRunner::class)
class SettingsActionCenterButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test fun openActionCenterButton_callsCallback() {
        var opened = 0
        rule.setContent { MyMetroUITheme { SettingsScreen(ThemeSettings(), {}, {}, {}, onOpenActionCenter = { opened++ }) } }
        rule.onNodeWithTag("open-action-center").performScrollTo().performClick()
        assertEquals(1, opened)
    }
}

@RunWith(RobolectricTestRunner::class)
class WelcomeSwitchTest {
    @get:Rule val rule = createComposeRule()

    @Test fun welcomeSwitch_togglesValue() {
        val calls = mutableListOf<Boolean>()
        rule.setContent {
            MyMetroUITheme {
                SettingsScreen(ThemeSettings(accentId = "red"), {}, {}, {}, welcomeEnabled = true, onWelcomeEnabled = { calls += it })
            }
        }
        rule.onNodeWithTag("welcome-switch").performScrollTo().performClick()
        assertEquals(listOf(false), calls)
    }
}
