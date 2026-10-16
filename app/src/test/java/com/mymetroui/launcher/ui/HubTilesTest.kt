package com.mymetroui.launcher.ui

import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.data.hub.HubKind
import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HubTilesTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val a = fakeApp("A", "p.a")

    private fun vm(tiles: FakeTileSource) = StartViewModel(tiles, FakeAppSource(listOf(a)))

    @Test fun hubKeyMapsToHubContent() {
        val vm = vm(FakeTileSource(listOf(Tile(1, "hub:photos", TileSize.MEDIUM, 0, 0))))
        assertEquals(TileContent.Hub(HubKind.PHOTOS), vm.state.value.single().content)
    }

    @Test fun unknownHubKey_skippedNotDeleted() {
        val tiles = FakeTileSource(listOf(Tile(1, "hub:future", TileSize.MEDIUM, 0, 0), Tile(2, a.key, TileSize.MEDIUM, 2, 0)))
        val vm = vm(tiles)
        assertEquals(listOf(2L), vm.state.value.map { it.tile.id })
        assertEquals(listOf("hub:future", a.key), tiles.flow.value.map { it.key })
    }

    @Test fun hubTileSurvivesPruneWithInstalledApps() {
        val tiles = FakeTileSource(listOf(Tile(1, "hub:photos", TileSize.MEDIUM, 0, 0)))
        vm(tiles)
        assertEquals(listOf("hub:photos"), tiles.flow.value.map { it.key })
    }

    @Test fun addHubPinsOnce() {
        val tiles = FakeTileSource()
        val vm = vm(tiles)
        vm.addHub(HubKind.PEOPLE)
        vm.addHub(HubKind.PEOPLE)
        assertEquals(1, tiles.flow.value.count { it.key == "hub:people" })
    }
}
