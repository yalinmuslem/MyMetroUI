package com.mymetroui.launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymetroui.launcher.data.AppInfo
import com.mymetroui.launcher.data.AppSource
import com.mymetroui.launcher.data.TileSource
import com.mymetroui.launcher.data.groupByLetter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppListViewModel(private val tiles: TileSource, private val apps: AppSource) : ViewModel() {
    val groups: StateFlow<Map<Char, List<AppInfo>>> = apps.apps.map(::groupByLetter)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val pinnedKeys: StateFlow<Set<String>> = tiles.tiles.map { l -> l.map { it.key }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun pin(key: String) {
        viewModelScope.launch { if (tiles.tiles.first().none { it.key == key }) tiles.pin(key) }
    }

    fun uninstall(app: AppInfo) = apps.uninstall(app)
}
