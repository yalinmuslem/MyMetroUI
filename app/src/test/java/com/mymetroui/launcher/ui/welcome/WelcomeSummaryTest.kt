package com.mymetroui.launcher.ui.welcome

import com.mymetroui.launcher.data.notif.NotifItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WelcomeSummaryTest {
    private fun item(key: String, title: String, whenMs: Long) =
        NotifItem(key, "p", 0, title, "", whenMs, true, 1, null, false)

    @Test fun notConnected_isNull() = assertNull(welcomeSummary(listOf(item("a", "A", 1)), connected = false, locked = false))

    @Test fun connectedEmpty_countZeroNoTitles() =
        assertEquals(WelcomeSummary(0, emptyList()), welcomeSummary(emptyList(), connected = true, locked = false))

    @Test fun countsAll_titlesAreThreeNewest() {
        val items = (1..5).map { item("k$it", "T$it", it.toLong()) }
        assertEquals(WelcomeSummary(5, listOf("T5", "T4", "T3")), welcomeSummary(items, connected = true, locked = false))
    }

    @Test fun blankTitlesSkipped() {
        val items = listOf(item("a", "", 3), item("b", "B", 2))
        assertEquals(WelcomeSummary(2, listOf("B")), welcomeSummary(items, connected = true, locked = false))
    }

    @Test fun locked_countOnly() {
        val items = listOf(item("a", "A", 1), item("b", "B", 2))
        assertEquals(WelcomeSummary(2, emptyList()), welcomeSummary(items, connected = true, locked = true))
    }
}
