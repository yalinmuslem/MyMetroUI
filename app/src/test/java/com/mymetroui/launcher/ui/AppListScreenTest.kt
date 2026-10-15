package com.mymetroui.launcher.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import com.mymetroui.launcher.data.AppInfo
import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.data.groupByLetter
import com.mymetroui.launcher.ui.applist.AppListScreen
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppListScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun show(
        apps: List<AppInfo>,
        onPin: (AppInfo) -> Unit = {},
        onUninstall: (AppInfo) -> Unit = {},
        fontScale: Float = 1f,
    ) = rule.setContent {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            MyMetroUITheme { AppListScreen(groupByLetter(apps), onLaunch = {}, onPin = onPin, onUninstall = onUninstall) }
        }
    }

    private val sample = listOf(fakeApp("1Password"), fakeApp("Alpha"), fakeApp("Beta"))

    @Test fun groupsAreShownUnderHeaders() {
        show(sample)
        listOf("#", "A", "B").forEach { rule.onNodeWithTag("header-$it").assertIsDisplayed() }
        rule.onNodeWithText("Alpha").assertIsDisplayed()
    }

    @Test fun tapHeader_showsJumpListWithDisabledLetters() {
        show(sample)
        rule.onNodeWithTag("header-A").performClick()
        rule.onNodeWithTag("jump-list").assertIsDisplayed()
        rule.onNodeWithTag("jump-B").assertHasClickAction()
        rule.onNodeWithTag("jump-C").assertHasNoClickAction()
    }

    @Test fun tapLetter_scrollsToGroup() {
        val many = ('A'..'Z').flatMap { c -> (1..6).map { fakeApp("$c app $it") } }
        show(many)
        rule.onNodeWithTag("header-A").performClick()
        rule.onNodeWithTag("jump-Z").performClick()
        rule.onNodeWithTag("header-Z").assertIsDisplayed()
        rule.onNodeWithTag("jump-list").assertDoesNotExist()
    }

    @Test fun longPressApp_showsPinAndUninstall() {
        var pinned: AppInfo? = null
        show(sample, onPin = { pinned = it })
        rule.onNodeWithText("Alpha").performTouchInput { longClick() }
        rule.onNodeWithText("Uninstall").assertIsDisplayed()
        rule.onNodeWithText("Pin to Start").performClick()
        assertEquals("Alpha", pinned?.label)
    }

    @Test fun appLabelWithLargeFontScale_ellipsizes() {
        show(listOf(fakeApp("An extremely long application label that cannot possibly fit on one line")), fontScale = 2f)
        rule.onNodeWithTag("header-A").assertIsDisplayed()
    }
}
