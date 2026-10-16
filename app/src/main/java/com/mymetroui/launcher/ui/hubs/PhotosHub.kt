package com.mymetroui.launcher.ui.hubs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.R

const val PhotosHubLimit = 60

@Composable
fun ColumnScope.PhotosHub(
    uris: List<String>,
    loadThumbnail: suspend (String) -> ImageBitmap?,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        stringResource(R.string.photos_add),
        Modifier.padding(vertical = 12.dp).background(scheme.primary).clickable(onClick = onAdd)
            .padding(horizontal = 20.dp, vertical = 12.dp).testTag("photos-add"),
        color = scheme.onPrimary,
    )
    if (uris.isEmpty()) {
        Text(stringResource(R.string.photos_empty), Modifier.testTag("photos-empty"), style = MaterialTheme.typography.bodyLarge)
        return
    }
    LazyVerticalGrid(
        GridCells.Fixed(3), Modifier.weight(1f).testTag("photos-grid"),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(uris.take(PhotosHubLimit)) { index, uri ->
            val bitmap = produceState<ImageBitmap?>(null, uri) { value = loadThumbnail(uri) }.value
            Box(Modifier.aspectRatio(1f).background(scheme.surfaceVariant).clickable { onOpen(uri) }.testTag("photo-$index")) {
                if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}
