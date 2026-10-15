package com.mymetroui.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultTilesTest {
    private val phone = fakeApp("Phone", "com.dialer")
    private val cam = fakeApp("Camera", "com.cam")

    @Test fun returnsOnlyInstalledAndSkipsMissing() {
        val resolve = { cat: String ->
            mapOf("phone" to "com.dialer", "camera" to "com.cam", "browser" to "com.gone")[cat]
        }
        assertEquals(listOf(phone.key, cam.key), defaultTileKeys(listOf(cam, phone), resolve))
    }

    @Test fun nothingResolved_isEmpty() {
        assertEquals(emptyList<String>(), defaultTileKeys(listOf(phone)) { null })
    }
}
