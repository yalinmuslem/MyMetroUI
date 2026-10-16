package com.mymetroui.launcher.ui

import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.mymetroui.launcher.data.AppInfo
import com.mymetroui.launcher.data.AppSource
import com.mymetroui.launcher.data.TileSource
import com.mymetroui.launcher.data.fakeApp
import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileLayout
import com.mymetroui.launcher.domain.TileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class FakeTileSource(initial: List<Tile> = emptyList()) : TileSource {
    val flow = MutableStateFlow(initial)
    private var nextId = 100L
    override val tiles get() = flow
    override suspend fun pin(key: String) { flow.value = flow.value + Tile(nextId++, key, TileSize.MEDIUM, 0, 0) }
    override suspend fun unpin(id: Long) { flow.value = flow.value.filter { it.id != id } }
    override suspend fun move(id: Long, col: Int, row: Int) { flow.value = TileLayout.move(flow.value, id, col, row) }
    override suspend fun resize(id: Long, size: TileSize) { flow.value = TileLayout.resize(flow.value, id, size) }
    override suspend fun removeKeys(keys: Set<String>) { flow.value = flow.value.filter { it.key !in keys } }
    override suspend fun compact() { flow.value = TileLayout.compact(flow.value) }
    override suspend fun isEmpty() = flow.value.isEmpty()
    override suspend fun seed(keys: List<String>) { keys.forEach { pin(it) } }
}

class FakeAppSource(apps: List<AppInfo>, var launchResult: Boolean = true) : AppSource {
    val flow = MutableStateFlow(apps)
    override val apps get() = flow
    override fun launch(app: AppInfo, sourceBounds: Rect?) = launchResult
    override fun loadIcon(app: AppInfo, density: Int): Drawable = throw UnsupportedOperationException()
    override fun resolveDefault(category: String): String? = if (category == "phone") "p.phone" else null
    override fun uninstall(app: AppInfo) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StartViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val a = fakeApp("A", "p.a")
    private val b = fakeApp("B", "p.b")

    @Test fun uninstalledApp_removesTile() = runBlocking {
        val tiles = FakeTileSource(listOf(Tile(1, a.key, TileSize.MEDIUM, 0, 0), Tile(2, b.key, TileSize.MEDIUM, 0, 4)))
        val apps = FakeAppSource(listOf(a, b))
        val vm = StartViewModel(tiles, apps)
        assertEquals(2, vm.state.value.size)
        apps.flow.value = listOf(a)
        assertEquals(listOf(a.key), vm.state.value.map { it.app!!.key })
        assertEquals(0, tiles.flow.value.single().row)
    }

    @Test fun emptyRepo_seedsDefaults() = runBlocking {
        val phone = fakeApp("Phone", "p.phone")
        val tiles = FakeTileSource()
        StartViewModel(tiles, FakeAppSource(listOf(phone, a)))
        assertEquals(listOf(phone.key), tiles.flow.value.map { it.key })
    }

    @Test fun launchFailure_removesTile() = runBlocking {
        val tiles = FakeTileSource(listOf(Tile(1, a.key, TileSize.MEDIUM, 0, 0)))
        val apps = FakeAppSource(listOf(a), launchResult = false)
        val vm = StartViewModel(tiles, apps)
        vm.launch(a, null)
        assertEquals(emptyList<Tile>(), tiles.flow.value)
    }

    @Test fun pin_alreadyPinned_noDuplicate() = runBlocking {
        val tiles = FakeTileSource(listOf(Tile(1, a.key, TileSize.MEDIUM, 0, 0)))
        val vm = StartViewModel(tiles, FakeAppSource(listOf(a)))
        vm.pin(a.key)
        assertEquals(1, tiles.flow.first().size)
    }
}

class FakeThemeSource(initial: com.mymetroui.launcher.data.ThemeSettings = com.mymetroui.launcher.data.ThemeSettings()) :
    com.mymetroui.launcher.data.ThemeSource {
    private val state = MutableStateFlow(initial)
    override val settings: kotlinx.coroutines.flow.StateFlow<com.mymetroui.launcher.data.ThemeSettings> = state
    override fun setAccent(id: String) { state.value = state.value.copy(accentId = id) }
    override fun setMode(mode: com.mymetroui.launcher.data.ThemeMode) { state.value = state.value.copy(mode = mode) }
}
