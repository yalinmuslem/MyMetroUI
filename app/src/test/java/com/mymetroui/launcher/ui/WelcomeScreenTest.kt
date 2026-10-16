package com.mymetroui.launcher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.geometry.Offset
import com.mymetroui.launcher.data.live.LiveFace
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import com.mymetroui.launcher.ui.welcome.WelcomeScreen
import com.mymetroui.launcher.ui.welcome.WelcomeSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WelcomeScreenTest {
    @get:Rule val rule = createComposeRule()

    private var dismissed = 0
    private val clock = LiveFace.Text("09:41", listOf("Fri, Oct 16"))

    private fun show(
        calendar: LiveFace? = null,
        summary: WelcomeSummary? = null,
        clockFace: LiveFace.Text? = clock,
    ) = rule.setContent {
        MyMetroUITheme { WelcomeScreen(clockFace, calendar, summary, onDismiss = { dismissed++ }) }
    }

    @Test fun showsClockAndDate() {
        show()
        rule.onNodeWithText("09:41").assertIsDisplayed()
        rule.onNodeWithText("Fri, Oct 16").assertIsDisplayed()
    }

    @Test fun calendarRow_shownForTextFace() {
        show(calendar = LiveFace.Text("Standup", listOf("10:00")))
        rule.onNodeWithTag("welcome-calendar").assertIsDisplayed()
        rule.onNodeWithText("Standup").assertIsDisplayed()
    }

    @Test fun calendarRow_hiddenWithoutPermission() {
        show(calendar = LiveFace.NeedsPermission)
        rule.onNodeWithTag("welcome-calendar").assertDoesNotExist()
    }

    @Test fun calendarRow_hiddenWhenNull() {
        show(calendar = null)
        rule.onNodeWithTag("welcome-calendar").assertDoesNotExist()
    }

    @Test fun notificationSection_hiddenWhenSummaryNull() {
        show(summary = null)
        rule.onNodeWithTag("welcome-notifs").assertDoesNotExist()
    }

    @Test fun showsCountAndTitles() {
        show(summary = WelcomeSummary(5, listOf("Alice", "Bob", "Carol")))
        rule.onNodeWithText("5 notifications").assertIsDisplayed()
        rule.onNodeWithText("Alice").assertIsDisplayed()
        rule.onNodeWithText("Carol").assertIsDisplayed()
    }

    @Test fun swipeUp_dismisses() {
        show()
        rule.onNodeWithTag("welcome-screen").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1, dismissed)
    }

    @Test fun smallSlowDrag_doesNotDismiss() {
        show()
        rule.onNodeWithTag("welcome-screen").performTouchInput {
            swipe(Offset(centerX, height * 0.5f), Offset(centerX, height * 0.4f), durationMillis = 1000)
        }
        rule.waitForIdle()
        assertEquals(0, dismissed)
        rule.onNodeWithTag("welcome-screen").assertIsDisplayed()
    }
}
