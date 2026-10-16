package com.mymetroui.launcher.data.live

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

data class CalendarEvent(val title: String, val startMs: Long, val allDay: Boolean)

interface CalendarQuery {
    fun hasPermission(): Boolean
    fun nextEvent(fromMs: Long, untilMs: Long): CalendarEvent?
    fun changes(): Flow<Unit>
}

class ContentResolverCalendarQuery(private val context: Context) : CalendarQuery {
    override fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    override fun nextEvent(fromMs: Long, untilMs: Long): CalendarEvent? {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, fromMs)
            ContentUris.appendId(it, untilMs)
        }.build()
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY),
            null, null, "${CalendarContract.Instances.BEGIN} ASC",
        ) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            return CalendarEvent(it.getString(0).orEmpty().ifBlank { "(No title)" }, it.getLong(1), it.getInt(2) == 1)
        }
    }

    override fun changes(): Flow<Unit> {
        if (!hasPermission()) return emptyFlow()
        return callbackFlow {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) { trySend(Unit) }
            }
            try {
                context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            } catch (_: SecurityException) {
                close()
            }
            awaitClose { context.contentResolver.unregisterContentObserver(observer) }
        }
    }
}
