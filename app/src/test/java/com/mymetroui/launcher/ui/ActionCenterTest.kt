package com.mymetroui.launcher.ui

import android.app.Notification
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mymetroui.launcher.data.notif.NotifItem
import com.mymetroui.launcher.data.quick.QuickKind
import com.mymetroui.launcher.ui.actioncenter.ActionCenter
import com.mymetroui.launcher.ui.actioncenter.ActionCenterActions
import com.mymetroui.launcher.ui.actioncenter.ActionCenterState
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActionCenterTest {
    @get:Rule val rule = createComposeRule()

    private val quick = mutableListOf<QuickKind>()
    private val dismissed = mutableListOf<String>()
    private val opened = mutableListOf<String>()
    private var clearAll = 0
    private var writeAccess = 0
    private var grant = 0

    private val actions = ActionCenterActions(
        onQuick = { quick += it }, onTorch = {}, onBrightness = {}, onRequestWriteSettings = { writeAccess++ },
        onDismiss = { dismissed += it }, onClearAll = { clearAll++ }, onOpen = { opened += it.key },
        onGrantAccess = { grant++ }, onClose = {},
    )

    private fun item(key: String, title: String = "Title $key", clearable: Boolean = true, visibility: Int = Notification.VISIBILITY_PUBLIC) =
        NotifItem(key, "p.$key", 0, title, "Body $key", 1, clearable, visibility, null, false)

    private fun state(
        items: List<NotifItem> = emptyList(),
        connected: Boolean = true,
        torchAvailable: Boolean = true,
        canWrite: Boolean = true,
        locked: Boolean = false,
    ) = ActionCenterState(items, connected, torchAvailable, false, 0.5f, canWrite, locked)

    private fun show(s: ActionCenterState) = rule.setContent { MyMetroUITheme { ActionCenter(s, actions) } }

    @Test fun listsNotifications() {
        show(state(listOf(item("a"), item("b"))))
        rule.onNodeWithText("Title a").assertIsDisplayed()
        rule.onNodeWithText("Body b").assertIsDisplayed()
    }

    @Test fun dismissButton_callsOnDismiss() {
        show(state(listOf(item("a"))))
        rule.onNodeWithTag("notif-dismiss-a").performClick()
        assertEquals(listOf("a"), dismissed)
    }

    @Test fun ongoingItem_hasNoDismiss() {
        show(state(listOf(item("a", clearable = false))))
        rule.onNodeWithTag("notif-a").assertIsDisplayed()
        rule.onNodeWithTag("notif-dismiss-a").assertDoesNotExist()
    }

    @Test fun clearAll_onlyWhenClearableExists() {
        show(state(listOf(item("a", clearable = false))))
        rule.onNodeWithTag("notif-clear-all").assertDoesNotExist()
    }

    @Test fun clearAll_callsCallback() {
        show(state(listOf(item("a"))))
        rule.onNodeWithTag("notif-clear-all").performClick()
        assertEquals(1, clearAll)
    }

    @Test fun emptyState_whenConnectedAndNoItems() {
        show(state())
        rule.onNodeWithTag("notif-empty").assertIsDisplayed()
    }

    @Test fun grantPrompt_whenNotConnected() {
        show(state(connected = false))
        rule.onNodeWithTag("notif-grant-access").performClick()
        assertEquals(1, grant)
        rule.onNodeWithTag("notif-empty").assertDoesNotExist()
    }

    @Test fun tapItem_callsOnOpen() {
        show(state(listOf(item("a"))))
        rule.onNodeWithTag("notif-a").performClick()
        assertEquals(listOf("a"), opened)
    }

    @Test fun torchDisabled_whenUnavailable() {
        show(state(torchAvailable = false))
        rule.onNodeWithTag("torch-toggle").assertIsNotEnabled()
    }

    @Test fun torchEnabled_whenAvailable() {
        show(state())
        rule.onNodeWithTag("torch-toggle").assertIsEnabled()
    }

    @Test fun brightnessWithoutPermission_requestsAccess() {
        show(state(canWrite = false))
        rule.onNodeWithTag("brightness-slider").performClick()
        assertEquals(1, writeAccess)
    }

    @Test fun quickButtons_callOnQuick() {
        show(state())
        QuickKind.values().forEach { rule.onNodeWithTag("quick-${it.name.lowercase()}").performClick() }
        assertEquals(QuickKind.values().toList(), quick)
    }

    @Test fun lockedPrivateItem_hidesContent() {
        show(state(listOf(item("a", visibility = Notification.VISIBILITY_PRIVATE), item("b")), locked = true))
        rule.onNodeWithText("Title a").assertDoesNotExist()
        rule.onNodeWithText("p.a").assertIsDisplayed()
        rule.onNodeWithText("Title b").assertIsDisplayed()
    }
}
