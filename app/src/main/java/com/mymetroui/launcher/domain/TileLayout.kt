package com.mymetroui.launcher.domain

object TileLayout {
    const val COLUMNS = 4

    fun isValid(tiles: List<Tile>): Boolean {
        if (tiles.map { it.id }.toSet().size != tiles.size) return false
        if (tiles.map { it.key }.toSet().size != tiles.size) return false
        if (tiles.any { it.col < 0 || it.row < 0 || it.col + it.size.w > COLUMNS }) return false
        return tiles.indices.all { i -> (i + 1 until tiles.size).none { j -> overlaps(tiles[i], tiles[j]) } }
    }

    fun place(tiles: List<Tile>, new: Tile): List<Tile> {
        var row = 0
        while (true) {
            for (col in 0..COLUMNS - new.size.w) {
                val candidate = new.copy(col = col, row = row)
                if (tiles.none { overlaps(it, candidate) }) return tiles + candidate
            }
            row++
        }
    }

    fun move(tiles: List<Tile>, id: Long, col: Int, row: Int): List<Tile> =
        change(tiles, id) { it.copy(col = clampCol(it.size, col), row = row.coerceAtLeast(0)) }

    fun resize(tiles: List<Tile>, id: Long, size: TileSize): List<Tile> =
        change(tiles, id) { it.copy(size = size, col = clampCol(size, it.col)) }

    fun compact(tiles: List<Tile>): List<Tile> {
        val placed = mutableListOf<Tile>()
        for (t in tiles.sortedWith(order)) {
            val row = (0..t.row).first { r -> placed.none { overlaps(it, t.copy(row = r)) } }
            placed += t.copy(row = row)
        }
        return placed
    }

    fun sanitize(tiles: List<Tile>): List<Tile> {
        val seen = HashSet<String>()
        val clamped = tiles.filter { seen.add(it.key) }
            .map { it.copy(col = clampCol(it.size, it.col), row = it.row.coerceAtLeast(0)) }
        return resolve(emptyList(), clamped)
    }

    private val order = compareBy<Tile>({ it.row }, { it.col })

    private fun clampCol(size: TileSize, col: Int) = col.coerceIn(0, COLUMNS - size.w)

    private fun overlaps(a: Tile, b: Tile) =
        a.col < b.col + b.size.w && b.col < a.col + a.size.w &&
            a.row < b.row + b.size.h && b.row < a.row + a.size.h

    private fun change(tiles: List<Tile>, id: Long, edit: (Tile) -> Tile): List<Tile> {
        val target = tiles.firstOrNull { it.id == id } ?: return tiles
        return resolve(listOf(edit(target)), tiles.filter { it.id != id })
    }

    /** Keeps [fixed] where it is and pushes each of [rest] (in row/col order) down until it overlaps nothing. */
    private fun resolve(fixed: List<Tile>, rest: List<Tile>): List<Tile> {
        val placed = fixed.toMutableList()
        for (t in rest.sortedWith(order)) {
            var cur = t
            while (true) {
                val hit = placed.filter { overlaps(it, cur) }
                if (hit.isEmpty()) break
                cur = cur.copy(row = hit.maxOf { it.row + it.size.h })
            }
            placed += cur
        }
        return placed
    }
}
