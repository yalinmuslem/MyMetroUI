package com.mymetroui.launcher.data.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class HubTileAdderTest {
    private val pinned = mutableSetOf<HubKind>()
    private val log = mutableListOf<HubKind>()
    private var requests = 0
    private var grant = true

    private val adder = HubTileAdder(
        pinned = { pinned },
        requestContacts = { cb -> requests++; cb(grant) },
        pin = { log += it; pinned += it },
    )

    @Test fun photosPinsImmediately() { adder.add(HubKind.PHOTOS); assertEquals(listOf(HubKind.PHOTOS), log) }

    @Test fun settingsPinsImmediately() { adder.add(HubKind.SETTINGS); assertEquals(listOf(HubKind.SETTINGS), log) }

    @Test fun peoplePinsWhenGranted() { adder.add(HubKind.PEOPLE); assertEquals(listOf(HubKind.PEOPLE), log) }

    @Test fun peopleNotPinnedWhenDenied() { grant = false; adder.add(HubKind.PEOPLE); assertEquals(emptyList<HubKind>(), log) }

    @Test fun alreadyPinned_isIgnored() {
        pinned += HubKind.PEOPLE
        adder.add(HubKind.PEOPLE)
        assertEquals(0, requests)
        assertEquals(emptyList<HubKind>(), log)
    }
}
