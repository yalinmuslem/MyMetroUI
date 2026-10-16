package com.mymetroui.launcher.data.notif

import org.junit.Assert.assertEquals
import org.junit.Test

class BadgeCountsTest {
    private fun item(key: String, pkg: String, user: Int = 0, clearable: Boolean = true) = NotifItem(
        key, pkg, user, "", "", 0, clearable, 1, null, false,
    )

    @Test fun countsPerPackageAndUser() {
        val counts = badgeCounts(listOf(item("1", "p.a"), item("2", "p.a"), item("3", "p.a", user = 10), item("4", "p.b")))
        assertEquals(2, counts[badgeKey("p.a", 0)])
        assertEquals(1, counts[badgeKey("p.a", 10)])
        assertEquals(1, counts[badgeKey("p.b", 0)])
    }

    @Test fun ongoingNotCounted() {
        val counts = badgeCounts(listOf(item("1", "p.a", clearable = false), item("2", "p.a")))
        assertEquals(1, counts[badgeKey("p.a", 0)])
    }

    @Test fun emptyIsEmptyMap() {
        assertEquals(emptyMap<String, Int>(), badgeCounts(emptyList()))
    }

    @Test fun keyFormat() {
        assertEquals("p.a#10", badgeKey("p.a", 10))
    }
}
