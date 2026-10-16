package com.mymetroui.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.R
import com.mymetroui.launcher.data.ThemeMode
import com.mymetroui.launcher.data.live.LiveKind
import com.mymetroui.launcher.data.ThemeSettings
import com.mymetroui.launcher.ui.theme.Accents

@Composable
fun SettingsScreen(
    settings: ThemeSettings,
    onAccent: (String) -> Unit,
    onMode: (ThemeMode) -> Unit,
    onClose: () -> Unit,
    pinnedLive: Set<LiveKind> = emptySet(),
    onAddLive: (LiveKind) -> Unit = {},
    onOpenActionCenter: () -> Unit = {},
    welcomeEnabled: Boolean = true,
    onWelcomeEnabled: (Boolean) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().background(scheme.background)
            .pointerInput(Unit) { detectTapGestures { } } // swallow touches meant for the page below
            .statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.theme_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
            Box(Modifier.size(48.dp).clickable(onClick = onClose).testTag("settings-close"), contentAlignment = Alignment.Center) {
                Text("×", style = MaterialTheme.typography.titleMedium)
            }
        }
        Row(Modifier.padding(vertical = 16.dp)) {
            ModeOption("mode-dark", stringResource(R.string.theme_dark), settings.mode == ThemeMode.DARK) { onMode(ThemeMode.DARK) }
            Box(Modifier.size(8.dp))
            ModeOption("mode-light", stringResource(R.string.theme_light), settings.mode == ThemeMode.LIGHT) { onMode(ThemeMode.LIGHT) }
        }
        Accents.chunked(4).forEach { row ->
            Row {
                row.forEach { accent ->
                    val selected = accent.id == settings.accentId
                    Box(
                        Modifier.padding(4.dp).size(64.dp).background(accent.color)
                            .then(if (selected) Modifier.border(3.dp, scheme.onBackground) else Modifier)
                            .testTag("accent-${accent.id}")
                            .selectable(selected = selected, onClick = { onAccent(accent.id) }),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.action_center),
            Modifier.padding(top = 24.dp).background(scheme.primary).clickable(onClick = onOpenActionCenter)
                .padding(horizontal = 20.dp, vertical = 12.dp).testTag("open-action-center"),
            color = scheme.onPrimary,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp).clickable { onWelcomeEnabled(!welcomeEnabled) }.testTag("welcome-switch"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.welcome_setting), Modifier.weight(1f))
            Switch(checked = welcomeEnabled, onCheckedChange = null)
        }
        Text(stringResource(R.string.live_tiles), Modifier.padding(top = 24.dp, bottom = 8.dp), style = MaterialTheme.typography.titleMedium)
        LiveKind.values().forEach { kind ->
            val isPinned = kind in pinnedLive
            Row(
                Modifier.padding(vertical = 4.dp)
                    .background(if (isPinned) scheme.surfaceVariant else scheme.primary)
                    .testTag("live-add-${kind.name.lowercase()}")
                    .clickable(enabled = !isPinned) { onAddLive(kind) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(stringResource(liveLabel(kind)), Modifier.weight(1f), color = if (isPinned) scheme.onSurfaceVariant else scheme.onPrimary)
                if (isPinned) Text(stringResource(R.string.live_on_start), color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ModeOption(tag: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.background(if (selected) scheme.primary else scheme.surfaceVariant)
            .testTag(tag)
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) { Text(label, color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant) }
}

private fun liveLabel(kind: LiveKind): Int = when (kind) {
    LiveKind.CLOCK -> R.string.live_clock
    LiveKind.CALENDAR -> R.string.live_calendar
    LiveKind.PHOTOS -> R.string.live_photos
}
