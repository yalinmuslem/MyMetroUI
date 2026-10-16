package com.mymetroui.launcher.data.hub

/** Add flow for hub tiles: People needs contacts access first; one tile per kind. */
class HubTileAdder(
    private val pinned: () -> Set<HubKind>,
    private val requestContacts: ((Boolean) -> Unit) -> Unit,
    private val pin: (HubKind) -> Unit,
) {
    fun add(kind: HubKind) {
        if (kind in pinned()) return
        when (kind) {
            HubKind.PEOPLE -> requestContacts { granted -> if (granted) pin(kind) }
            HubKind.PHOTOS, HubKind.SETTINGS -> pin(kind)
        }
    }
}
