package com.mymetroui.launcher.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** On/off switch for the welcome screen; on by default. */
class WelcomePrefs(private val prefs: SharedPreferences) {
    private val state = MutableStateFlow(prefs.getBoolean(KEY, true))
    val enabled: StateFlow<Boolean> = state

    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean(KEY, on).apply()
        state.value = on
    }

    private companion object { const val KEY = "enabled" }
}
