package com.mymetroui.launcher.domain

enum class TileSize(val w: Int, val h: Int) { SMALL(1, 1), MEDIUM(2, 2), WIDE(4, 2) }

data class Tile(val id: Long, val key: String, val size: TileSize, val col: Int, val row: Int)

/** Resize-button cycle: SMALL -> MEDIUM -> WIDE -> SMALL. */
fun TileSize.next(): TileSize = TileSize.values()[(ordinal + 1) % TileSize.values().size]
