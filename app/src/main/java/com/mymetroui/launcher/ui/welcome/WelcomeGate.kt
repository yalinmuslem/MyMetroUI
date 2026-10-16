package com.mymetroui.launcher.ui.welcome

/**
 * Turns screen-off signals into a one-shot "show the welcome screen" decision.
 * SCREEN_OFF and onPause can arrive in either order, so either one flags it.
 */
class WelcomeGate(private val enabled: () -> Boolean) {
    private var resumed = false
    private var pending = false

    fun onScreenOff() { if (resumed) pending = true }

    fun onPause(interactive: Boolean) {
        resumed = false
        if (!interactive) pending = true
    }

    /** True when the welcome screen should show now; always consumes the flag. */
    fun onResume(): Boolean {
        resumed = true
        val show = pending && enabled()
        pending = false
        return show
    }
}
