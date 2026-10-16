package com.mymetroui.launcher.data.notif

import android.app.Notification
import android.text.SpannableString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationMappingTest {
    private fun raw(
        key: String,
        pkg: String = "p.a",
        title: CharSequence? = "T",
        text: CharSequence? = "X",
        whenMs: Long = 1,
        flags: Int = 0,
        userId: Int = 0,
    ) = RawNotif(key, pkg, userId, title, text, whenMs, flags, Notification.VISIBILITY_PUBLIC, null)

    @Test fun mapsTitleTextAndFlags() {
        val item = mapNotifications(
            listOf(raw("k", flags = Notification.FLAG_AUTO_CANCEL)),
            ownPackage = "me",
        ).single()
        assertEquals("T", item.title)
        assertEquals("X", item.text)
        assertTrue(item.clearable)
        assertTrue(item.autoCancel)
    }

    @Test fun ongoingAndNoClearAreNotClearable() {
        val items = mapNotifications(
            listOf(raw("a", flags = Notification.FLAG_ONGOING_EVENT), raw("b", flags = Notification.FLAG_NO_CLEAR), raw("c")),
            "me",
        )
        assertEquals(listOf(false, false, true), items.sortedBy { it.key }.map { it.clearable })
    }

    @Test fun ownPackageAndGroupSummaryDropped() {
        val items = mapNotifications(
            listOf(raw("own", pkg = "me"), raw("sum", flags = Notification.FLAG_GROUP_SUMMARY), raw("ok")),
            "me",
        )
        assertEquals(listOf("ok"), items.map { it.key })
    }

    @Test fun nullAndSpannedTextAreBlankSafe() {
        val items = mapNotifications(listOf(raw("a", title = null, text = SpannableString("hi")), raw("b", title = null, text = null)), "me")
        assertEquals("", items.first { it.key == "a" }.title)
        assertEquals("hi", items.first { it.key == "a" }.text)
        assertEquals("", items.first { it.key == "b" }.text)
    }

    @Test fun newestFirst() {
        val items = mapNotifications(listOf(raw("old", whenMs = 1), raw("new", whenMs = 9), raw("mid", whenMs = 5)), "me")
        assertEquals(listOf("new", "mid", "old"), items.map { it.key })
    }

    private class FakeController : NotificationController {
        val cancelled = mutableListOf<String>()
        val cancelledAll = mutableListOf<List<String>>()
        override fun cancel(key: String) { cancelled += key }
        override fun cancelAll(keys: List<String>) { cancelledAll += keys }
    }

    @Test fun repository_onDisconnected_clearsAndFlagsDisconnected() = runBlocking {
        val repo = NotificationRepository()
        repo.onConnected(listOf(raw("a")), "me", FakeController())
        assertTrue(repo.connected.first())
        assertEquals(1, repo.items.first().size)
        repo.onDisconnected()
        assertFalse(repo.connected.first())
        assertEquals(emptyList<NotifItem>(), repo.items.first())
    }

    @Test fun repository_clearAll_passesOnlyClearableKeys() {
        val c = FakeController()
        val repo = NotificationRepository()
        repo.onConnected(listOf(raw("a"), raw("b", flags = Notification.FLAG_ONGOING_EVENT), raw("c")), "me", c)
        repo.clearAll()
        assertEquals(listOf(setOf("a", "c")), c.cancelledAll.map { it.toSet() })
    }

    @Test fun repository_dismiss_forwardsAndIsNoOpWhenDisconnected() {
        val c = FakeController()
        val repo = NotificationRepository()
        repo.dismiss("x") // never connected
        repo.onConnected(listOf(raw("a")), "me", c)
        repo.dismiss("a")
        repo.onDisconnected()
        repo.dismiss("a")
        assertEquals(listOf("a"), c.cancelled)
    }

    @Test fun repository_onChanged_replacesItems() {
        val repo = NotificationRepository()
        repo.onConnected(listOf(raw("a")), "me", FakeController())
        repo.onChanged(listOf(raw("b"), raw("c")), "me")
        assertEquals(setOf("b", "c"), repo.items.value.map { it.key }.toSet())
    }
}
