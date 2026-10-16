package com.mymetroui.launcher.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset

/** Every duration, easing and angle used by launcher motion. Starting points; tune by eye. */
object MotionSpec {
    const val TiltMaxDegrees = 8f
    const val TiltPressedScale = 0.945f
    val TiltReturnSpring = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)

    const val TurnstileDurationMs = 350
    const val TurnstileStaggerMs = 25
    const val TurnstileAngle = 90f
    const val FlyInDurationMs = 400

    const val FlipDurationMs = 600
    const val FlipHoldMs = 3500
    const val FlipIntervalMs = 9000
    const val FlipStaggerMs = 700

    const val ActionCenterDurationMs = 300
    const val ActionCenterOpenFraction = 0.25f

    const val WelcomeDurationMs = 300

    const val ParallaxTileFactor = 1.0f
    const val ParallaxBackgroundFactor = 0.3f

    const val EditScale = 0.9f
    const val EditLiftScale = 1.05f
    const val EditDimAlpha = 0.6f
    val ReflowSpring = spring<IntOffset>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
}
