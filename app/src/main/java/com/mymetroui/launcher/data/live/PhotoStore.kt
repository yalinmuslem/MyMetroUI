package com.mymetroui.launcher.data.live

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PhotoStore(private val prefs: SharedPreferences) {
    private val version = MutableStateFlow(0)

    /** Bumps whenever the list changes so sources can refresh. */
    val changes: StateFlow<Int> = version

    fun uris(): List<String> {
        val raw = try { prefs.getString(KEY, null) } catch (_: ClassCastException) { null } ?: return emptyList()
        return raw.split('\n').map { it.trim() }.filter { it.startsWith("content://") }
    }

    fun add(uris: List<String>) {
        val merged = (this.uris() + uris.map { it.trim() }.filter { it.startsWith("content://") }).distinct()
        prefs.edit().putString(KEY, merged.joinToString("\n")).apply()
        version.value++
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
        version.value++
    }

    private companion object { const val KEY = "photos" }
}
