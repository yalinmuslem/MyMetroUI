package com.mymetroui.launcher.ui

import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymetroui.launcher.data.AppInfo
import com.mymetroui.launcher.data.AppSource
import com.mymetroui.launcher.data.TileSource
import com.mymetroui.launcher.data.defaultTileKeys
import com.mymetroui.launcher.data.hub.HubKind
import com.mymetroui.launcher.data.live.LiveKind
import com.mymetroui.launcher.data.live.LiveTileSource
import com.mymetroui.launcher.data.live.NoLiveTiles
import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TileContent {
    data class App(val app: AppInfo) : TileContent
    data class Live(val kind: LiveKind) : TileContent
    data class Hub(val kind: HubKind) : TileContent
}

data class TileUi(val tile: Tile, val content: TileContent) {
    constructor(tile: Tile, app: AppInfo) : this(tile, TileContent.App(app))

    val app: AppInfo? get() = (content as? TileContent.App)?.app
}

class StartViewModel(
    private val tiles: TileSource,
    private val apps: AppSource,
    val live: LiveTileSource = NoLiveTiles,
) : ViewModel() {
    private val joined = combine(tiles.tiles, apps.apps) { t, a ->
        val byKey = a.associateBy { it.key }
        t to byKey
    }

    val state: StateFlow<List<TileUi>> = joined
        .map { (t, byKey) -> t.mapNotNull { tile -> contentFor(tile.key, byKey)?.let { TileUi(tile, it) } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Drop tiles whose app is gone (uninstalled, or component removed by an update).
        viewModelScope.launch {
            joined.collect { (t, byKey) ->
                val missing = t.filter { !it.key.startsWith(LiveKind.PREFIX) && !it.key.startsWith(HubKind.PREFIX) && it.key !in byKey }.map { it.key }.toSet()
                if (missing.isNotEmpty() && byKey.isNotEmpty()) {
                    tiles.removeKeys(missing)
                    tiles.compact()
                }
            }
        }
        // shortcut: reseeds defaults whenever Start is empty at process start, even if the user cleared it
        viewModelScope.launch {
            val installed = apps.apps.first { it.isNotEmpty() }
            if (tiles.isEmpty()) tiles.seed(defaultTileKeys(installed, apps::resolveDefault))
        }
    }

    fun pin(key: String) {
        viewModelScope.launch { if (tiles.tiles.first().none { it.key == key }) tiles.pin(key) }
    }

    fun addLive(kind: LiveKind) = pin(kind.key)
    fun addHub(kind: HubKind) = pin(kind.key)

    fun unpin(id: Long) { viewModelScope.launch { tiles.unpin(id) } }
    fun move(id: Long, col: Int, row: Int) { viewModelScope.launch { tiles.move(id, col, row) } }
    fun resize(id: Long, size: TileSize) { viewModelScope.launch { tiles.resize(id, size) } }
    fun onEditFinished() { viewModelScope.launch { tiles.compact() } }

    fun launch(app: AppInfo, bounds: Rect?): Boolean {
        val ok = apps.launch(app, bounds)
        if (!ok) viewModelScope.launch { tiles.removeKeys(setOf(app.key)); tiles.compact() }
        return ok
    }
}

// Unknown live:* and hub:* keys (e.g. written by a newer version) map to null: skipped, never deleted.
private fun contentFor(key: String, apps: Map<String, AppInfo>): TileContent? =
    if (key.startsWith(LiveKind.PREFIX)) LiveKind.fromKey(key)?.let { TileContent.Live(it) }
    else if (key.startsWith(HubKind.PREFIX)) HubKind.fromKey(key)?.let { TileContent.Hub(it) }
    else apps[key]?.let { TileContent.App(it) }
