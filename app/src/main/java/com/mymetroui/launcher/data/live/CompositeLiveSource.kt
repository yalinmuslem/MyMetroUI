package com.mymetroui.launcher.data.live

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

/** Each source owns distinct kinds and returns an empty flow for the others. */
class CompositeLiveSource(private vararg val sources: LiveTileSource) : LiveTileSource {
    override fun face(kind: LiveKind): Flow<LiveFace?> = merge(*sources.map { it.face(kind) }.toTypedArray())
    override fun onFlipped(kind: LiveKind) = sources.forEach { it.onFlipped(kind) }
}
