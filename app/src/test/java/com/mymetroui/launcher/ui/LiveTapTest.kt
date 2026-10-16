package com.mymetroui.launcher.ui

import android.content.Intent
import android.provider.AlarmClock
import com.mymetroui.launcher.data.live.LiveFace
import com.mymetroui.launcher.data.live.LiveKind
import com.mymetroui.launcher.data.live.LiveTap
import com.mymetroui.launcher.data.live.liveOpenIntent
import com.mymetroui.launcher.data.live.liveTap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LiveTapTest {
    @Test fun calendarNeedsPermission_requestsInsteadOfOpening() {
        assertEquals(LiveTap.RequestCalendar, liveTap(LiveKind.CALENDAR, LiveFace.NeedsPermission))
    }

    @Test fun calendarWithFace_opens() {
        assertEquals(LiveTap.Open(LiveKind.CALENDAR), liveTap(LiveKind.CALENDAR, LiveFace.Text("x")))
        assertEquals(LiveTap.Open(LiveKind.CALENDAR), liveTap(LiveKind.CALENDAR, null))
    }

    @Test fun clockAndPhotos_alwaysOpen() {
        assertEquals(LiveTap.Open(LiveKind.CLOCK), liveTap(LiveKind.CLOCK, LiveFace.NeedsPermission))
        assertEquals(LiveTap.Open(LiveKind.PHOTOS), liveTap(LiveKind.PHOTOS, null))
    }

    @Test fun openIntents_haveExpectedActions() {
        assertEquals(AlarmClock.ACTION_SHOW_ALARMS, liveOpenIntent(LiveKind.CLOCK).action)
        val cal = liveOpenIntent(LiveKind.CALENDAR)
        assertEquals(Intent.ACTION_VIEW, cal.action)
        assertTrue(cal.data.toString().startsWith("content://com.android.calendar/time/"))
        val photos = liveOpenIntent(LiveKind.PHOTOS)
        assertEquals(Intent.ACTION_MAIN, photos.action)
        assertTrue(photos.hasCategory(Intent.CATEGORY_APP_GALLERY))
    }
}
