package com.mymetroui.launcher.data

import com.mymetroui.launcher.data.db.TileDao
import com.mymetroui.launcher.data.db.TileEntity
import com.mymetroui.launcher.domain.Tile
import com.mymetroui.launcher.domain.TileLayout
import com.mymetroui.launcher.domain.TileSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TileRepository(private val dao: TileDao) : TileSource {
    private val lock = Mutex()

    override val tiles: Flow<List<Tile>> = dao.observeAll().map { TileLayout.sanitize(it.map(::toTile)) }

    override suspend fun pin(key: String) = mutate { l ->
        if (l.any { it.key == key }) l else TileLayout.place(l, Tile(0, key, TileSize.MEDIUM, 0, 0))
    }

    override suspend fun unpin(id: Long) = mutate { l -> TileLayout.compact(l.filter { it.id != id }) }
    override suspend fun move(id: Long, col: Int, row: Int) = mutate { TileLayout.move(it, id, col, row) }
    override suspend fun resize(id: Long, size: TileSize) = mutate { TileLayout.resize(it, id, size) }
    override suspend fun removeKeys(keys: Set<String>) = mutate { l -> l.filter { it.key !in keys } }
    override suspend fun compact() = mutate { TileLayout.compact(it) }
    override suspend fun isEmpty(): Boolean = tiles.first().isEmpty()
    override suspend fun seed(keys: List<String>) = mutate { l ->
        keys.fold(l) { acc, k -> if (acc.any { it.key == k }) acc else TileLayout.place(acc, Tile(0, k, TileSize.MEDIUM, 0, 0)) }
    }

    private suspend fun mutate(change: (List<Tile>) -> List<Tile>) = lock.withLock {
        val next = change(tiles.first())
        dao.replaceAll(next.map { TileEntity(it.id, it.key, it.size.name, it.col, it.row) })
    }

    private fun toTile(e: TileEntity) = Tile(
        e.id, e.componentKey, TileSize.values().firstOrNull { it.name == e.size } ?: TileSize.MEDIUM, e.col, e.row,
    )
}
