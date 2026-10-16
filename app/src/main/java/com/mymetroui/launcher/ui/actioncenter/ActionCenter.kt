package com.mymetroui.launcher.ui.actioncenter

import android.app.Notification
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.R
import com.mymetroui.launcher.data.notif.NotifItem
import com.mymetroui.launcher.data.quick.QuickKind

@Composable
fun ActionCenter(state: ActionCenterState, actions: ActionCenterActions, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().background(scheme.surfaceVariant).statusBarsPadding().padding(16.dp)) {
        Row(Modifier.fillMaxWidth()) {
            QuickKind.values().forEach { kind ->
                QuickButton("quick-${kind.name.lowercase()}", stringResource(quickLabel(kind)), enabled = true, on = false) {
                    actions.onQuick(kind)
                }
            }
            QuickButton("torch-toggle", stringResource(R.string.torch), enabled = state.torchAvailable, on = state.torchOn) {
                actions.onTorch(!state.torchOn)
            }
        }

        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            if (state.canWriteSettings) {
                Slider(state.brightness, actions.onBrightness, Modifier.testTag("brightness-slider"))
            } else {
                // without WRITE_SETTINGS the slider cannot write; touching it opens the access screen instead
                Slider(state.brightness, {}, Modifier.fillMaxWidth(), enabled = false)
                Box(Modifier.matchParentSize().clickable(onClick = actions.onRequestWriteSettings).testTag("brightness-slider"))
            }
        }

        when {
            !state.connected -> Text(
                stringResource(R.string.grant_notification_access),
                Modifier.fillMaxWidth().clickable(onClick = actions.onGrantAccess).padding(vertical = 12.dp).testTag("notif-grant-access"),
                style = MaterialTheme.typography.bodyLarge,
            )
            state.items.isEmpty() -> Text(
                stringResource(R.string.no_notifications),
                Modifier.padding(vertical = 12.dp).testTag("notif-empty"),
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> {
                if (state.items.any { it.clearable }) {
                    Text(
                        stringResource(R.string.clear_all),
                        Modifier.clickable(onClick = actions.onClearAll).padding(vertical = 8.dp).testTag("notif-clear-all"),
                        color = scheme.primary,
                    )
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(state.items, key = { it.key }) { NotificationRow(it, state.deviceLocked, actions) }
                }
            }
        }
    }
}

@Composable
private fun QuickButton(tag: String, label: String, enabled: Boolean, on: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.padding(end = 8.dp).background(if (on) scheme.primary else scheme.background)
            .testTag(tag)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, color = if (on) scheme.onPrimary else scheme.onBackground)
    }
}

@Composable
private fun NotificationRow(item: NotifItem, locked: Boolean, actions: ActionCenterActions) {
    val hidden = locked && item.visibility != Notification.VISIBILITY_PUBLIC
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable { actions.onOpen(item) }.padding(vertical = 4.dp).testTag("notif-${item.key}")) {
            if (hidden) {
                Text(item.packageName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.text, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (item.clearable) {
            Box(
                Modifier.size(40.dp).clickable { actions.onDismiss(item.key) }.testTag("notif-dismiss-${item.key}"),
                contentAlignment = Alignment.Center,
            ) { Text("×") }
        }
    }
}

private fun quickLabel(kind: QuickKind): Int = when (kind) {
    QuickKind.WIFI -> R.string.quick_wifi
    QuickKind.BLUETOOTH -> R.string.quick_bluetooth
    QuickKind.AIRPLANE -> R.string.quick_airplane
    QuickKind.LOCATION -> R.string.quick_location
}
