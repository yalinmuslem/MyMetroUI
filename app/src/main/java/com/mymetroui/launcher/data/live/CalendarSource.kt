package com.mymetroui.launcher.data.live

import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

class CalendarSource(
    private val query: CalendarQuery,
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
    private val wait: suspend (Long) -> Unit = { delay(it) },
) : LiveTileSource {
    override fun face(kind: LiveKind): Flow<LiveFace?> =
        if (kind != LiveKind.CALENDAR) emptyFlow()
        else flow { merge(minuteTicks(now, wait), query.changes()).collect { emit(compute()) } }

    private fun compute(): LiveFace? {
        if (!query.hasPermission()) return LiveFace.NeedsPermission
        return try {
            val n = now() // "today" is recomputed on every refresh so midnight and zone changes are honoured
            val until = n.toLocalDate().plusDays(1).atStartOfDay(n.zone).toInstant().toEpochMilli()
            val event = query.nextEvent(n.toInstant().toEpochMilli(), until)
                ?: return LiveFace.Text("No events today")
            val time = if (event.allDay) "All day"
            else DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(event.startMs).atZone(n.zone))
            LiveFace.Text(event.title, listOf(time))
        } catch (_: SecurityException) { // permission revoked while running
            LiveFace.NeedsPermission
        } catch (_: Exception) {
            null
        }
    }
}
