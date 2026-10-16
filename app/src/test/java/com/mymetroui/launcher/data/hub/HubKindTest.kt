package com.mymetroui.launcher.data.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HubKindTest {
    @Test fun keysRoundTrip() = HubKind.values().forEach { assertEquals(it, HubKind.fromKey(it.key)) }

    @Test fun keys() = assertEquals(listOf("hub:people", "hub:photos", "hub:settings"), HubKind.values().map { it.key })

    @Test fun unknownKeyIsNull() = assertNull(HubKind.fromKey("hub:future"))

    @Test fun liveKeyIsNull() = assertNull(HubKind.fromKey("live:clock"))
}
