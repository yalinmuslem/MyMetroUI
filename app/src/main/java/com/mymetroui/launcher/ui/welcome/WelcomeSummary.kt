package com.mymetroui.launcher.ui.welcome

import com.mymetroui.launcher.data.notif.NotifItem

data class WelcomeSummary(val count: Int, val titles: List<String>)

/** Null when notification access is off. A locked device shows the count only. */
fun welcomeSummary(items: List<NotifItem>, connected: Boolean, locked: Boolean): WelcomeSummary? {
    if (!connected) return null
    val titles = if (locked) emptyList()
    else items.sortedByDescending { it.whenMs }.map { it.title }.filter { it.isNotBlank() }.take(3)
    return WelcomeSummary(items.size, titles)
}
