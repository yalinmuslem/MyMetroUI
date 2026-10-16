package com.mymetroui.launcher.ui.actioncenter

import android.app.Notification
import com.mymetroui.launcher.data.notif.NotificationController
import com.mymetroui.launcher.data.notif.NotificationRepository
import com.mymetroui.launcher.data.notif.RawNotif
import com.mymetroui.launcher.data.quick.Brightness
import com.mymetroui.launcher.data.quick.Torch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class FakeTorch(override val available: Boolean = true) : Torch {
    override val isOn = MutableStateFlow(false)
    override fun set(on: Boolean) { isOn.value = on }
}

class FakeBrightness(var allowed: Boolean = true, var value: Float = 0.5f) : Brightness {
    val writes = mutableListOf<Float>()
    override fun canWrite() = allowed
    override fun read() = value
    override fun write(fraction: Float) { writes += fraction; value = fraction }
}

private class RecordingController : NotificationController {
    val cancelled = mutableListOf<String>()
    val cancelledAll = mutableListOf<List<String>>()
    override fun cancel(key: String) { cancelled += key }
    override fun cancelAll(keys: List<String>) { cancelledAll += keys }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ActionCenterControllerTest {
    private val repo = NotificationRepository()
    private val torch = FakeTorch()
    private val brightness = FakeBrightness()
    private val notifController = RecordingController()
    private var locked = false

    private fun raw(key: String, flags: Int = 0) =
        RawNotif(key, "p.a", 0, "T", "X", 1, flags, Notification.VISIBILITY_PUBLIC, null)

    private fun controller() = ActionCenterController(
        repo, torch, brightness, { locked }, CoroutineScope(UnconfinedTestDispatcher()),
    )

    @Test fun state_reflectsNotificationsAndTorch() {
        val c = controller()
        repo.onConnected(listOf(raw("a")), "me", notifController)
        c.setTorch(true)
        val s = c.state.value
        assertEquals(listOf("a"), s.items.map { it.key })
        assertTrue(s.connected)
        assertTrue(s.torchOn)
        assertTrue(s.torchAvailable)
    }

    @Test fun setBrightness_withoutPermission_doesNotWrite() {
        brightness.allowed = false
        controller().setBrightness(0.9f)
        assertEquals(emptyList<Float>(), brightness.writes)
    }

    @Test fun setBrightness_withPermission_writes() {
        val c = controller()
        c.setBrightness(0.9f)
        assertEquals(listOf(0.9f), brightness.writes)
        assertEquals(0.9f, c.state.value.brightness, 0f)
    }

    @Test fun refresh_picksUpRevokedWritePermissionAndLock() {
        val c = controller()
        assertTrue(c.state.value.canWriteSettings)
        brightness.allowed = false
        locked = true
        c.refresh()
        assertFalse(c.state.value.canWriteSettings)
        assertTrue(c.state.value.deviceLocked)
    }

    @Test fun dismissAndClearAll_forwardToRepository() {
        val c = controller()
        repo.onConnected(listOf(raw("a"), raw("b", Notification.FLAG_ONGOING_EVENT), raw("c")), "me", notifController)
        c.dismiss("a")
        c.clearAll()
        assertEquals(listOf("a"), notifController.cancelled)
        assertEquals(setOf("a", "c"), notifController.cancelledAll.single().toSet())
    }

    @Test fun disconnected_stateIsEmptyAndNotConnected() {
        val c = controller()
        repo.onConnected(listOf(raw("a")), "me", notifController)
        repo.onDisconnected()
        assertEquals(emptyList<Any>(), c.state.value.items)
        assertFalse(c.state.value.connected)
    }
}
