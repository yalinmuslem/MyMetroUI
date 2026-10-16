package com.mymetroui.launcher.data.live

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeQuery(
    var permission: Boolean = true,
    var event: CalendarEvent? = null,
    var failure: Exception? = null,
) : CalendarQuery {
    var lastWindow: Pair<Long, Long>? = null
    override fun hasPermission() = permission
    override fun nextEvent(fromMs: Long, untilMs: Long): CalendarEvent? {
        lastWindow = fromMs to untilMs
        failure?.let { throw it }
        return event
    }
    override fun changes(): Flow<Unit> = emptyFlow()
}

class CalendarSourceTest {
    private val zone = ZoneId.of("America/New_York")
    private val now = ZonedDateTime.of(2026, 10, 15, 9, 0, 0, 0, zone)

    private fun face(q: CalendarQuery) = runBlocking {
        CalendarSource(q, now = { now }, wait = {}).face(LiveKind.CALENDAR).first()
    }

    @Test fun noPermission_needsPermission() {
        assertEquals(LiveFace.NeedsPermission, face(FakeQuery(permission = false)))
    }

    @Test fun nextEvent_mapsTitleAndTime() {
        val start = now.plusHours(2).toInstant().toEpochMilli()
        val f = face(FakeQuery(event = CalendarEvent("Standup", start, allDay = false))) as LiveFace.Text
        assertEquals("Standup", f.title)
        assertEquals(1, f.lines.size)
        assertEquals(true, f.lines[0].startsWith("11:00") || f.lines[0].startsWith("11"))
    }

    @Test fun allDayEvent_showsAllDay() {
        val f = face(FakeQuery(event = CalendarEvent("Holiday", 0, allDay = true))) as LiveFace.Text
        assertEquals(listOf("All day"), f.lines)
    }

    @Test fun noEvents_showsNone() {
        assertEquals(LiveFace.Text("No events today"), face(FakeQuery()))
    }

    @Test fun queryThrows_emitsNull() {
        assertEquals(null, face(FakeQuery(failure = IllegalStateException("boom"))))
    }

    @Test fun securityException_needsPermission() {
        assertEquals(LiveFace.NeedsPermission, face(FakeQuery(failure = SecurityException())))
    }

    @Test fun window_endsAtLocalMidnight() {
        val q = FakeQuery()
        val dstEnd = ZonedDateTime.of(2026, 11, 1, 10, 0, 0, 0, zone) // 25-hour day
        runBlocking { CalendarSource(q, now = { dstEnd }, wait = {}).face(LiveKind.CALENDAR).first() }
        val midnight = ZonedDateTime.of(2026, 11, 2, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(midnight, q.lastWindow!!.second)
        assertEquals(dstEnd.toInstant().toEpochMilli(), q.lastWindow!!.first)
    }
}
