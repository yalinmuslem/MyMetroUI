package com.mymetroui.launcher.data

import android.content.SharedPreferences
import com.mymetroui.launcher.ui.theme.Accents
import com.mymetroui.launcher.ui.theme.DefaultAccent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ThemeSource {
    val settings: StateFlow<ThemeSettings>
    fun setAccent(id: String)
    fun setMode(mode: ThemeMode)
}

class ThemeRepository(private val prefs: SharedPreferences) : ThemeSource {
    private val state = MutableStateFlow(load())
    override val settings: StateFlow<ThemeSettings> = state

    override fun setAccent(id: String) {
        if (Accents.none { it.id == id }) return
        prefs.edit().putString(ACCENT, id).apply()
        state.value = state.value.copy(accentId = id)
    }

    override fun setMode(mode: ThemeMode) {
        prefs.edit().putString(MODE, mode.name).apply()
        state.value = state.value.copy(mode = mode)
    }

    // A stored value that is missing, of the wrong type or unknown falls back to the default for that field only.
    private fun load(): ThemeSettings {
        val accent = readString(ACCENT)?.takeIf { id -> Accents.any { it.id == id } } ?: DefaultAccent.id
        val mode = ThemeMode.values().firstOrNull { it.name == readString(MODE) } ?: ThemeMode.DARK
        return ThemeSettings(accent, mode)
    }

    private fun readString(key: String): String? = try {
        prefs.getString(key, null)
    } catch (_: ClassCastException) {
        null
    }

    private companion object {
        const val ACCENT = "accent"
        const val MODE = "mode"
    }
}
