package com.mymetroui.launcher.ui.hubs

import java.io.FileNotFoundException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThumbnailsTest {
    @Test fun sampleSizes() {
        assertEquals(8, sampleSize(4000, 3000, 256))
        assertEquals(1, sampleSize(300, 300, 256))
        assertEquals(2, sampleSize(512, 512, 256))
        assertEquals(1, sampleSize(100, 100, 256))
    }

    @Test fun unreadableUri_returnsNull() = runBlocking {
        assertNull(decodeThumbnail({ throw FileNotFoundException() }, "content://nope/1"))
    }

    @Test fun missingStream_returnsNull() = runBlocking {
        assertNull(decodeThumbnail({ null }, "content://nope/1"))
    }
}
