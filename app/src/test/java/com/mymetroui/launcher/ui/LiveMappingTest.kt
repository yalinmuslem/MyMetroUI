package com.mymetroui.launcher.ui

import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.data.live.LiveKind
import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LiveMappingTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val a = fakeApp("A", "p.a")
    private fun tile(id: Long, key: String, row: Int) = Tile(id, key, TileSize.MEDIUM, 0, row)

    @Test fun liveKey_mapsToLiveContent() = runBlocking {
        val tiles = FakeTileSource(listOf(tile(1, "live:clock", 0), tile(2, a.key, 2)))
        val vm = StartViewModel(tiles, FakeAppSource(listOf(a)))
        assertEquals(TileContent.Live(LiveKind.CLOCK), vm.state.value[0].content)
        assertEquals(a.key, vm.state.value[1].app?.key)
    }

    @Test fun liveTile_notRemovedWhenAppListChanges() = runBlocking {
        val tiles = FakeTileSource(listOf(tile(1, "live:clock", 0), tile(2, a.key, 2)))
        val apps = FakeAppSource(listOf(a))
        StartViewModel(tiles, apps)
        apps.flow.value = listOf(fakeApp("B", "p.b"))
        assertEquals(listOf("live:clock"), tiles.flow.value.map { it.key })
    }

    @Test fun unknownLiveKey_skippedAndNotDeleted() = runBlocking {
        val tiles = FakeTileSource(listOf(tile(1, "live:weather", 0), tile(2, a.key, 2)))
        val vm = StartViewModel(tiles, FakeAppSource(listOf(a)))
        assertEquals(listOf(a.key), vm.state.value.map { it.app?.key })
        assertEquals(2, tiles.flow.value.size)
    }

    @Test fun addLive_pinsKey_onceOnly() = runBlocking {
        val tiles = FakeTileSource()
        val vm = StartViewModel(tiles, FakeAppSource(listOf(a)))
        tiles.flow.value = emptyList()
        vm.addLive(LiveKind.CALENDAR)
        vm.addLive(LiveKind.CALENDAR)
        assertEquals(listOf("live:calendar"), tiles.flow.value.map { it.key })
    }

    @Test fun fromKey() {
        assertEquals(LiveKind.CLOCK, LiveKind.fromKey("live:clock"))
        assertNull(LiveKind.fromKey("live:nope"))
        assertNull(LiveKind.fromKey("a/B#0"))
    }
}
