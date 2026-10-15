package com.mymetroui.launcher.data

import android.content.ComponentName
import android.os.UserHandle
import java.text.Normalizer

data class AppInfo(
    val key: String,
    val label: String,
    val userId: Int,
    val component: ComponentName,
    val user: UserHandle,
)

fun makeKey(component: ComponentName, userId: Int): String = "${component.packageName}/${component.className}#$userId"

/** Groups by first letter; empty or non-letter labels go under '#', which sorts first. */
fun groupByLetter(apps: List<AppInfo>): Map<Char, List<AppInfo>> =
    apps.groupBy { a -> a.label.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#' }
        .toSortedMap(compareBy<Char> { it != '#' }.thenBy { baseLetter(it) }.thenBy { it })

// 'É' sorts with 'E'
private fun baseLetter(c: Char): Char = Normalizer.normalize(c.toString(), Normalizer.Form.NFD).first()
