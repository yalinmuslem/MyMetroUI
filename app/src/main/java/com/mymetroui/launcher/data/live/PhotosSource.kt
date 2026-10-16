package com.mymetroui.launcher.data.live

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow

class PhotosSource(private val store: PhotoStore, private val canOpen: (String) -> Boolean) : LiveTileSource {
    private val index = MutableStateFlow(0)

    override fun face(kind: LiveKind): Flow<LiveFace?> =
        if (kind != LiveKind.PHOTOS) emptyFlow()
        else combine(index, store.changes) { i, _ ->
            val openable = store.uris().filter { runCatching { canOpen(it) }.getOrDefault(false) }
            if (openable.isEmpty()) LiveFace.Text("Pick photos") else LiveFace.Photo(openable[i.mod(openable.size)])
        }

    override fun onFlipped(kind: LiveKind) {
        if (kind == LiveKind.PHOTOS) index.value++
    }
}
