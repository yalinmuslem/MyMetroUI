package com.mymetroui.launcher.ui.hubs

import com.mymetroui.launcher.R
import com.mymetroui.launcher.data.hub.HubKind

fun hubLabel(kind: HubKind): Int = when (kind) {
    HubKind.PEOPLE -> R.string.hub_people
    HubKind.PHOTOS -> R.string.hub_photos
    HubKind.SETTINGS -> R.string.hub_settings
}
