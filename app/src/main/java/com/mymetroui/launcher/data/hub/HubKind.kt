package com.mymetroui.launcher.data.hub

enum class HubKind {
    PEOPLE, PHOTOS, SETTINGS;

    val key: String get() = PREFIX + name.lowercase()

    companion object {
        const val PREFIX = "hub:"
        fun fromKey(key: String): HubKind? = values().firstOrNull { it.key == key }
    }
}
