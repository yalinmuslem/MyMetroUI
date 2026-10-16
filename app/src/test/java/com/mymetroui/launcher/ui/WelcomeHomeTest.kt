package com.mymetroui.launcher.ui

import android.app.Notification
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.data.notif.NotificationController
import com.mymetroui.launcher.data.notif.NotificationRepository
import com.mymetroui.launcher.data.notif.RawNotif
import com.mymetroui.launcher.ui.actioncenter.ActionCenterController
import com.mymetroui.launcher.ui.actioncenter.FakeBrightness
import com.mymetroui.launcher.ui.actioncenter.FakeTorch
import com.mymetroui.launcher.ui.home.HomeScreen
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import kotlinx.coroutines.CoroutineScope
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
class WelcomeHomeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private var dismissed = 0

    private fun controller(locked: Boolean = false): ActionCenterController {
        val repo = NotificationRepository()
        repo.onConnected(
            listOf(RawNotif("a", "p.a", 0, "Secret title", "x", 1, 0, Notification.VISIBILITY_PUBLIC, null)), "me",
            object : NotificationController {
                override fun cancel(key: String) {}
                override fun cancelAll(keys: List<String>) {}
            },
        )
        return ActionCenterController(repo, FakeTorch(), FakeBrightness(), { locked }, CoroutineScope(UnconfinedTestDispatcher()))
    }

    private fun showHome(visible: Boolean, locked: Boolean = false) {
        val tiles = FakeTileSource(emptyList())
        val apps = FakeAppSource(listOf(fakeApp("Alpha", "p.alpha")))
        val start = StartViewModel(tiles, apps)
        val appList = AppListViewModel(tiles, apps)
        rule.setContent {
            MyMetroUITheme {
                HomeScreen(
                    start, appList, theme = FakeThemeSource(), loadIcon = { ColorDrawable(0) },
                    controller = controller(locked), welcomeVisible = visible, onWelcomeDismiss = { dismissed++ },
                )
            }
        }
    }

    @Test fun visible_showsWelcomeScreen() {
        showHome(true)
        rule.onNodeWithTag("welcome-screen").assertIsDisplayed()
    }

    @Test fun notVisible_noWelcomeScreen() {
        showHome(false)
        rule.onNodeWithTag("welcome-screen").assertDoesNotExist()
    }

    @Test fun visible_actionCenterEdgeAbsent() {
        showHome(true)
        rule.onNodeWithTag("action-center-edge").assertDoesNotExist()
    }

    @Test fun swipeUp_callsOnWelcomeDismiss() {
        showHome(true)
        rule.onNodeWithTag("welcome-screen").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test fun back_callsOnWelcomeDismiss() {
        showHome(true)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test fun unlockedDevice_showsTitle() {
        showHome(true, locked = false)
        rule.onNodeWithText("Secret title").assertIsDisplayed()
    }

    @Test fun lockedDevice_hidesTitles() {
        showHome(true, locked = true)
        rule.onNodeWithText("1 notification").assertIsDisplayed()
        rule.onNodeWithText("Secret title").assertDoesNotExist()
    }
}
