package com.mymetroui.launcher.data.live

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

fun clockFace(now: ZonedDateTime, locale: Locale = Locale.getDefault()): LiveFace.Text = LiveFace.Text(
    title = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(now),
    lines = listOf(DateTimeFormatter.ofPattern("EEE, MMM d", locale).format(now)),
)

fun millisToNextMinute(nowMs: Long): Long = 60_000 - nowMs.mod(60_000L)

/** Emits immediately, then once per wall-clock minute. [wait] is injectable so tests do not sleep. */
internal fun minuteTicks(now: () -> ZonedDateTime, wait: suspend (Long) -> Unit): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        wait(millisToNextMinute(now().toInstant().toEpochMilli()))
    }
}

class ClockSource(
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
    private val wait: suspend (Long) -> Unit = { delay(it) },
) : LiveTileSource {
    override fun face(kind: LiveKind): Flow<LiveFace?> =
        if (kind != LiveKind.CLOCK) emptyFlow()
        else flow { minuteTicks(now, wait).collect { emit(clockFace(now())) } } // re-read the clock every tick
}
