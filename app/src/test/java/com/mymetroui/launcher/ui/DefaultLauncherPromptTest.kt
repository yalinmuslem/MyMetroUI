package com.mymetroui.launcher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.mymetroui.launcher.ui.home.DefaultLauncherPrompt
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultLauncherPromptTest {
    @get:Rule val rule = createComposeRule()

    @Test fun prompt_shownWhenNotDefault() {
        var chosen = 0
        rule.setContent { MyMetroUITheme { DefaultLauncherPrompt(isDefault = false, onChoose = { chosen++ }) } }
        rule.onNodeWithTag("default-prompt").assertIsDisplayed()
        rule.onNodeWithTag("default-prompt-choose").performClick()
        assertEquals(1, chosen)
    }

    @Test fun prompt_hiddenWhenDefault() {
        rule.setContent { MyMetroUITheme { DefaultLauncherPrompt(isDefault = true, onChoose = {}) } }
        rule.onNodeWithTag("default-prompt").assertDoesNotExist()
    }
}
