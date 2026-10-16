package com.mymetroui.launcher.ui.actioncenter

import com.mymetroui.launcher.data.notif.NotifItem
import com.mymetroui.launcher.data.notif.NotificationRepository
import com.mymetroui.launcher.data.quick.Brightness
import com.mymetroui.launcher.data.quick.QuickKind
import com.mymetroui.launcher.data.quick.Torch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ActionCenterState(
    val items: List<NotifItem>,
    val connected: Boolean,
    val torchAvailable: Boolean,
    val torchOn: Boolean,
    val brightness: Float,
    val canWriteSettings: Boolean,
    val deviceLocked: Boolean,
)

data class ActionCenterActions(
    val onQuick: (QuickKind) -> Unit,
    val onTorch: (Boolean) -> Unit,
    val onBrightness: (Float) -> Unit,
    val onRequestWriteSettings: () -> Unit,
    val onDismiss: (String) -> Unit,
    val onClearAll: () -> Unit,
    val onOpen: (NotifItem) -> Unit,
    val onGrantAccess: () -> Unit,
    val onClose: () -> Unit,
)

class ActionCenterController(
    private val notifs: NotificationRepository,
    private val torch: Torch,
    private val brightness: Brightness,
    private val isDeviceLocked: () -> Boolean,
    scope: CoroutineScope,
) {
    private val refreshTick = MutableStateFlow(0)

    private fun snapshot(items: List<NotifItem>, connected: Boolean, torchOn: Boolean) = ActionCenterState(
        items = items,
        connected = connected,
        torchAvailable = torch.available,
        torchOn = torchOn,
        brightness = brightness.read(),
        canWriteSettings = brightness.canWrite(),
        deviceLocked = isDeviceLocked(),
    )

    val state: StateFlow<ActionCenterState> =
        combine(notifs.items, notifs.connected, torch.isOn, refreshTick) { items, connected, torchOn, _ ->
            snapshot(items, connected, torchOn)
        }.stateIn(scope, SharingStarted.Eagerly, snapshot(notifs.items.value, notifs.connected.value, torch.isOn.value))

    /** Re-reads permission, brightness and lock state; call when the panel opens. */
    fun refresh() { refreshTick.value++ }

    fun setTorch(on: Boolean) = torch.set(on)

    fun setBrightness(fraction: Float) {
        if (!brightness.canWrite()) return
        brightness.write(fraction)
        refresh()
    }

    fun dismiss(key: String) = notifs.dismiss(key)
    fun clearAll() = notifs.clearAll()
}
