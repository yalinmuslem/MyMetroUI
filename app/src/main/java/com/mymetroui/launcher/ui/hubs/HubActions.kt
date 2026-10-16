package com.mymetroui.launcher.ui.hubs

import androidx.compose.ui.graphics.ImageBitmap
import com.mymetroui.launcher.data.hub.Contact
import com.mymetroui.launcher.data.hub.ContactsQuery
import com.mymetroui.launcher.data.live.PhotoStore

/** Android-facing hooks for the hubs, injected so each hub can be tested with fakes. */
data class HubActions(
    val photoStore: PhotoStore? = null,
    val loadThumbnail: suspend (String) -> ImageBitmap? = { null },
    val onOpenPhoto: (String) -> Unit = {},
    val onAddPhotos: () -> Unit = {},
    val contacts: ContactsQuery? = null,
    val contactsVersion: Int = 0,
    val onRequestContacts: () -> Unit = {},
    val onOpenContact: (Contact) -> Unit = {},
)
