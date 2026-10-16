package com.mymetroui.launcher.data.notif

fun badgeKey(packageName: String, userId: Int): String = "$packageName#$userId"

/** Only clearable notifications count, so ongoing ones (music, navigation) never raise a badge. */
fun badgeCounts(items: List<NotifItem>): Map<String, Int> =
    items.filter { it.clearable }.groupingBy { badgeKey(it.packageName, it.userId) }.eachCount()
