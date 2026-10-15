package com.mymetroui.launcher.data

import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileSize
import kotlinx.coroutines.flow.Flow

interface TileSource {
    val tiles: Flow<List<Tile>>
    suspend fun pin(key: String)
    suspend fun unpin(id: Long)
    suspend fun move(id: Long, col: Int, row: Int)
    suspend fun resize(id: Long, size: TileSize)
    suspend fun removeKeys(keys: Set<String>)
    suspend fun compact()
    suspend fun isEmpty(): Boolean
    suspend fun seed(keys: List<String>)
}

interface AppSource {
    val apps: Flow<List<AppInfo>>
    fun launch(app: AppInfo, sourceBounds: Rect?): Boolean
    fun loadIcon(app: AppInfo, density: Int): Drawable
    fun resolveDefault(category: String): String?
    fun uninstall(app: AppInfo)
}
