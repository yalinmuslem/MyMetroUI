package com.mymetroui.launcher.data.notif

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface NotificationController {
    fun cancel(key: String)
    fun cancelAll(keys: List<String>)
}

/** In-memory only: notification content is never persisted or sent anywhere. */
class NotificationRepository {
    private val _items = MutableStateFlow<List<NotifItem>>(emptyList())
    private val _connected = MutableStateFlow(false)
    @Volatile private var controller: NotificationController? = null

    val items: StateFlow<List<NotifItem>> = _items
    val connected: StateFlow<Boolean> = _connected

    fun onConnected(raw: List<RawNotif>, ownPackage: String, controller: NotificationController) {
        this.controller = controller
        _items.value = mapNotifications(raw, ownPackage)
        _connected.value = true
    }

    fun onChanged(raw: List<RawNotif>, ownPackage: String) {
        _items.value = mapNotifications(raw, ownPackage)
    }

    fun onDisconnected() {
        controller = null
        _items.value = emptyList()
        _connected.value = false
    }

    fun dismiss(key: String) { controller?.cancel(key) }

    fun clearAll() {
        controller?.cancelAll(_items.value.filter { it.clearable }.map { it.key })
    }
}
