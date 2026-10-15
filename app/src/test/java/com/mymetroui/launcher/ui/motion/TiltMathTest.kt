package com.mymetroui.launcher.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TiltMathTest {
    private val size = Size(200f, 100f)

    @Test fun center_isZero() {
        val (x, y) = tiltFor(Offset(100f, 50f), size)
        assertEquals(0f, x, 0f); assertEquals(0f, y, 0f)
    }

    @Test fun topLeft_bothNonzeroWithinMax() {
        val (x, y) = tiltFor(Offset(0f, 0f), size)
        assertTrue(x != 0f && y != 0f)
        assertTrue(abs(x) <= MotionSpec.TiltMaxDegrees && abs(y) <= MotionSpec.TiltMaxDegrees)
    }

    @Test fun rightEdgeMidpoint_onlyRotatesAroundY() {
        val (x, y) = tiltFor(Offset(200f, 50f), size)
        assertEquals(0f, x, 0f)
        assertTrue(y != 0f)
    }

    @Test fun outsideBounds_isClamped() {
        val (x, y) = tiltFor(Offset(5000f, -5000f), size)
        assertTrue(abs(x) <= MotionSpec.TiltMaxDegrees && abs(y) <= MotionSpec.TiltMaxDegrees)
        assertEquals(tiltFor(Offset(200f, 0f), size), tiltFor(Offset(5000f, -5000f), size))
    }
}
