package com.mymetroui.launcher.data.live

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock

/** Add flow for live tiles: permission or picker first, pin only on success, one tile per kind. */
class LiveTileAdder(
    private val pinned: () -> Set<LiveKind>,
    private val requestCalendar: ((Boolean) -> Unit) -> Unit,
    private val pickPhotos: ((List<String>) -> Unit) -> Unit,
    private val store: PhotoStore,
    private val pin: (LiveKind) -> Unit,
) {
    fun add(kind: LiveKind) {
        if (kind in pinned()) return
        when (kind) {
            LiveKind.CLOCK -> pin(kind)
            LiveKind.CALENDAR -> requestCalendar { granted -> if (granted) pin(kind) }
            LiveKind.PHOTOS -> pickPhotos { uris ->
                if (uris.isNotEmpty()) {
                    store.add(uris)
                    pin(kind)
                }
            }
        }
    }
}

sealed interface LiveTap {
    data class Open(val kind: LiveKind) : LiveTap
    object RequestCalendar : LiveTap
}

fun liveTap(kind: LiveKind, face: LiveFace?): LiveTap =
    if (kind == LiveKind.CALENDAR && face is LiveFace.NeedsPermission) LiveTap.RequestCalendar else LiveTap.Open(kind)

fun liveOpenIntent(kind: LiveKind): Intent = when (kind) {
    LiveKind.CLOCK -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
    LiveKind.CALENDAR -> Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}"))
    LiveKind.PHOTOS -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY)
}
