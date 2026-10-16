package com.mymetroui.launcher.data.live

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoStoreTest {
    private val prefs get() = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("photos-test", Context.MODE_PRIVATE)

    @Before fun setUp() { prefs.edit().clear().commit() }

    @Test fun addAndReadKeepsOrderAndDedupes() {
        val s = PhotoStore(prefs)
        s.add(listOf("content://a/1", "content://a/2"))
        s.add(listOf("content://a/2", "content://a/3"))
        assertEquals(listOf("content://a/1", "content://a/2", "content://a/3"), s.uris())
    }

    @Test fun blankAndNonContentEntriesDropped() {
        prefs.edit().putString("photos", "content://a/1\n\n  \nfile:///x\nhttp://y\ncontent://a/2").commit()
        assertEquals(listOf("content://a/1", "content://a/2"), PhotoStore(prefs).uris())
    }

    @Test fun persistsAcrossInstances() {
        PhotoStore(prefs).add(listOf("content://a/1"))
        assertEquals(listOf("content://a/1"), PhotoStore(prefs).uris())
    }

    @Test fun wrongTypeInPrefs_isEmpty() {
        prefs.edit().putInt("photos", 4).commit()
        assertEquals(emptyList<String>(), PhotoStore(prefs).uris())
    }

    private fun source(uris: List<String>, open: Set<String>): PhotosSource {
        val s = PhotoStore(prefs).also { it.clear(); it.add(uris) }
        return PhotosSource(s) { it in open }
    }

    @Test fun rotatesOnFlip() = runBlocking {
        val src = source(listOf("content://a/1", "content://a/2"), setOf("content://a/1", "content://a/2"))
        assertEquals(LiveFace.Photo("content://a/1"), src.face(LiveKind.PHOTOS).first())
        src.onFlipped(LiveKind.PHOTOS)
        assertEquals(LiveFace.Photo("content://a/2"), src.face(LiveKind.PHOTOS).first())
        src.onFlipped(LiveKind.PHOTOS)
        assertEquals(LiveFace.Photo("content://a/1"), src.face(LiveKind.PHOTOS).first())
    }

    @Test fun unreadableSkipped() = runBlocking {
        val src = source(listOf("content://a/1", "content://a/2"), setOf("content://a/2"))
        assertEquals(LiveFace.Photo("content://a/2"), src.face(LiveKind.PHOTOS).first())
    }

    @Test fun noneOpenable_showsPickPhotos() = runBlocking {
        val src = source(listOf("content://a/1"), emptySet())
        assertEquals(LiveFace.Text("Pick photos"), src.face(LiveKind.PHOTOS).first())
        assertEquals(LiveFace.Text("Pick photos"), source(emptyList(), emptySet()).face(LiveKind.PHOTOS).first())
    }

    @Test fun composite_routesByKind() = runBlocking {
        val photos = source(listOf("content://a/1"), setOf("content://a/1"))
        val composite = CompositeLiveSource(ClockSource(), photos)
        assertEquals(LiveFace.Photo("content://a/1"), composite.face(LiveKind.PHOTOS).first())
    }
}
