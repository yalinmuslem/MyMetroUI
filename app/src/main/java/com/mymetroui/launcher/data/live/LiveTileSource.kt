package com.mymetroui.launcher.data.live

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface LiveTileSource {
    /** The back face of a live tile; null means there is nothing to show and the tile stays static. */
    fun face(kind: LiveKind): Flow<LiveFace?>
    fun onFlipped(kind: LiveKind) {}
}

object NoLiveTiles : LiveTileSource {
    override fun face(kind: LiveKind): Flow<LiveFace?> = emptyFlow()
}
