package com.mymetroui.launcher.ui.actioncenter

import com.mymetroui.launcher.ui.motion.MotionSpec

object PanelDrag {
    const val FlingVelocity = 1200f

    /** Decides whether the panel ends open (true) or closed (false) once the finger lifts. */
    fun settle(fraction: Float, velocityPxPerSec: Float, wasOpen: Boolean): Boolean {
        val f = fraction.coerceIn(0f, 1f)
        return when {
            velocityPxPerSec > FlingVelocity -> true
            velocityPxPerSec < -FlingVelocity -> false
            wasOpen -> f > 1f - MotionSpec.ActionCenterOpenFraction
            else -> f >= MotionSpec.ActionCenterOpenFraction
        }
    }
}
