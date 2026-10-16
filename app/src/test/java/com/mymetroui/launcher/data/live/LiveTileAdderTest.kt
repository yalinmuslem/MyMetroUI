package com.mymetroui.launcher.data.live

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LiveTileAdderTest {
    private lateinit var store: PhotoStore
    private val pinned = mutableSetOf<LiveKind>()
    private val pins = mutableListOf<LiveKind>()
    private var calendarAsked = 0
    private var pickerOpened = 0
    private var calendarAnswer = true
    private var pickAnswer: List<String> = emptyList()

    @Before fun setUp() {
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("adder-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        store = PhotoStore(prefs)
    }

    private fun adder() = LiveTileAdder(
        pinned = { pinned },
        requestCalendar = { cb -> calendarAsked++; cb(calendarAnswer) },
        pickPhotos = { cb -> pickerOpened++; cb(pickAnswer) },
        store = store,
        pin = { pins += it },
    )

    @Test fun clock_pinsImmediately() {
        adder().add(LiveKind.CLOCK)
        assertEquals(listOf(LiveKind.CLOCK), pins)
        assertEquals(0, calendarAsked)
    }

    @Test fun calendar_grantedPins_deniedDoesNot() {
        calendarAnswer = false
        adder().add(LiveKind.CALENDAR)
        assertEquals(emptyList<LiveKind>(), pins)
        calendarAnswer = true
        adder().add(LiveKind.CALENDAR)
        assertEquals(listOf(LiveKind.CALENDAR), pins)
    }

    @Test fun photos_pickedStoresAndPins() {
        pickAnswer = listOf("content://a/1")
        adder().add(LiveKind.PHOTOS)
        assertEquals(listOf("content://a/1"), store.uris())
        assertEquals(listOf(LiveKind.PHOTOS), pins)
    }

    @Test fun photos_emptyPickDoesNotPin() {
        adder().add(LiveKind.PHOTOS)
        assertEquals(emptyList<LiveKind>(), pins)
        assertEquals(emptyList<String>(), store.uris())
    }

    @Test fun alreadyPinned_isNoOp() {
        pinned += LiveKind.values()
        LiveKind.values().forEach { adder().add(it) }
        assertEquals(emptyList<LiveKind>(), pins)
        assertEquals(0, calendarAsked)
        assertEquals(0, pickerOpened)
    }
}
