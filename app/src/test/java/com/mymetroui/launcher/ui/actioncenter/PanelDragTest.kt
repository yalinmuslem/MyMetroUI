package com.mymetroui.launcher.ui.actioncenter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelDragTest {
    @Test fun smallDragFromClosed_staysClosed() = assertFalse(PanelDrag.settle(0.1f, 0f, wasOpen = false))

    @Test fun pastThresholdFromClosed_opens() {
        assertTrue(PanelDrag.settle(0.25f, 0f, wasOpen = false))
        assertTrue(PanelDrag.settle(0.6f, 0f, wasOpen = false))
    }

    @Test fun fastFlingOpens_evenWithSmallFraction() =
        assertTrue(PanelDrag.settle(0.05f, PanelDrag.FlingVelocity + 1, wasOpen = false))

    @Test fun fastUpFlingCloses_evenWhenMostlyOpen() =
        assertFalse(PanelDrag.settle(0.9f, -PanelDrag.FlingVelocity - 1, wasOpen = true))

    @Test fun closeDragFromOpen_needsMoreThanQuarter() {
        assertTrue(PanelDrag.settle(0.8f, 0f, wasOpen = true))
        assertFalse(PanelDrag.settle(0.7f, 0f, wasOpen = true))
    }

    @Test fun fractionClamped() {
        assertFalse(PanelDrag.settle(-1f, 0f, wasOpen = false))
        assertTrue(PanelDrag.settle(5f, 0f, wasOpen = true))
    }
}
