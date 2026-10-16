package com.mymetroui.launcher.data.live

enum class LiveKind {
    CLOCK, CALENDAR, PHOTOS;

    val key: String get() = PREFIX + name.lowercase()

    companion object {
        const val PREFIX = "live:"
        fun fromKey(key: String): LiveKind? = values().firstOrNull { it.key == key }
    }
}
