package com.mymetroui.launcher.data.live

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockSourceTest {
    private val utc = ZoneId.of("UTC")

    @Test fun clockFace_formatsTimeAndDate() {
        val face = clockFace(ZonedDateTime.of(2026, 10, 15, 14, 5, 0, 0, utc), Locale.US)
        assertTrue(face.title, face.title.startsWith("2:05"))
        assertEquals(listOf("Thu, Oct 15"), face.lines)
    }

    @Test fun millisToNextMinute_isWithinOneMinute() {
        assertEquals(1L, millisToNextMinute(59_999))
        assertEquals(60_000L, millisToNextMinute(60_000))
        assertEquals(60_000L, millisToNextMinute(0))
    }

    @Test fun face_recomputedFromWallClock() = runBlocking {
        var now = ZonedDateTime.of(2026, 10, 15, 23, 59, 0, 0, utc)
        val source = ClockSource(now = { now }, wait = { now = now.plusMinutes(1) })
        val faces = source.face(LiveKind.CLOCK).take(2).toList()
        assertNotEquals(faces[0], faces[1])
        assertEquals(listOf("Fri, Oct 16"), (faces[1] as LiveFace.Text).lines) // crossed midnight
    }

    @Test fun otherKinds_areEmpty() = runBlocking {
        assertEquals(emptyList<LiveFace?>(), ClockSource().face(LiveKind.CALENDAR).toList())
    }
}
