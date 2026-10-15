package com.mymetroui.launcher.data

private val DEFAULT_CATEGORIES = listOf("phone", "messaging", "browser", "camera", "gallery")

/** [resolveDefaults] maps a category to the package name of the system's default handler. */
fun defaultTileKeys(apps: List<AppInfo>, resolveDefaults: (category: String) -> String?): List<String> =
    DEFAULT_CATEGORIES.mapNotNull { cat ->
        val pkg = resolveDefaults(cat) ?: return@mapNotNull null
        apps.firstOrNull { it.component.packageName == pkg }?.key
    }.distinct()
