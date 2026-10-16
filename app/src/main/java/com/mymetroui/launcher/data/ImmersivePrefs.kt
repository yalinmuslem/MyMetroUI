package com.mymetroui.launcher.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Whether the launcher hides the system bars while it is in front; off by default. */
class ImmersivePrefs(private val prefs: SharedPreferences) {
    private val state = MutableStateFlow(prefs.getBoolean(KEY, false))
    val enabled: StateFlow<Boolean> = state

    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean(KEY, on).apply()
        state.value = on
    }

    private companion object { const val KEY = "enabled" }
}
