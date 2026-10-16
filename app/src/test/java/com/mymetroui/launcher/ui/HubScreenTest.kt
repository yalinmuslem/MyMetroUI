package com.mymetroui.launcher.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mymetroui.launcher.data.hub.HubKind
import com.mymetroui.launcher.data.quick.QuickKind
import com.mymetroui.launcher.ui.actioncenter.ActionCenterActions
import com.mymetroui.launcher.ui.actioncenter.ActionCenterState
import com.mymetroui.launcher.ui.hubs.HubScreen
import com.mymetroui.launcher.ui.hubs.QuickSettingsHub
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HubScreenTest {
    @get:Rule val rule = createComposeRule()

    private var closed = 0

    private fun show() = rule.setContent {
        MyMetroUITheme { HubScreen(HubKind.PHOTOS, { closed++ }) { Text("body") } }
    }

    @Test fun showsTitle() {
        show()
        rule.onNodeWithTag("hub-title").assertIsDisplayed()
        rule.onNodeWithText("Photos").assertIsDisplayed()
    }

    @Test fun closeCallsCallback() {
        show()
        rule.onNodeWithTag("hub-close").performClick()
        assertEquals(1, closed)
    }

    @Test fun contentIsShown() {
        show()
        rule.onNodeWithText("body").assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
class QuickSettingsHubTest {
    @get:Rule val rule = createComposeRule()

    private val quick = mutableListOf<QuickKind>()
    private val torch = mutableListOf<Boolean>()
    private var settings = 0

    private val actions = ActionCenterActions(
        onQuick = { quick += it }, onTorch = { torch += it }, onBrightness = {}, onRequestWriteSettings = {},
        onDismiss = {}, onClearAll = {}, onOpen = {}, onGrantAccess = {}, onClose = {},
    )

    private fun show() = rule.setContent {
        MyMetroUITheme {
            HubScreen(HubKind.SETTINGS, {}) {
                QuickSettingsHub(ActionCenterState(emptyList(), true, true, false, 0.5f, true, false), actions) { settings++ }
            }
        }
    }

    @Test fun quickButtonsCallOnQuick() {
        show()
        QuickKind.values().forEach { rule.onNodeWithTag("quick-${it.name.lowercase()}").performClick() }
        assertEquals(QuickKind.values().toList(), quick)
    }

    @Test fun torchToggle() {
        show()
        rule.onNodeWithTag("torch-toggle").performClick()
        assertEquals(listOf(true), torch)
    }

    @Test fun openSettingsCallsBack() {
        show()
        rule.onNodeWithTag("hub-open-settings").performClick()
        assertEquals(1, settings)
    }
}
