package com.mymetroui.launcher.ui.welcome

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeGateTest {
    private var on = true
    private val gate = WelcomeGate { on }

    @Test fun screenOffWhileResumed_showsOnNextResume() {
        assertFalse(gate.onResume())
        gate.onScreenOff()
        gate.onPause(false)
        assertTrue(gate.onResume())
    }

    @Test fun pauseWhileNotInteractive_showsOnNextResume() {
        gate.onResume()
        gate.onPause(false)
        assertTrue(gate.onResume())
    }

    @Test fun pauseWhileInteractive_doesNotShow() {
        gate.onResume()
        gate.onPause(true)
        assertFalse(gate.onResume())
    }

    @Test fun coldStart_doesNotShow() = assertFalse(gate.onResume())

    @Test fun screenOffWhilePaused_doesNotFlag() {
        gate.onResume()
        gate.onPause(true)
        gate.onScreenOff()
        assertFalse(gate.onResume())
    }

    @Test fun flagIsConsumed() {
        gate.onResume()
        gate.onPause(false)
        assertTrue(gate.onResume())
        gate.onPause(true)
        assertFalse(gate.onResume())
    }

    @Test fun switchOff_holdsAndClears() {
        gate.onResume()
        gate.onPause(false)
        on = false
        assertFalse(gate.onResume())
        on = true
        gate.onPause(true)
        assertFalse(gate.onResume())
    }
}
