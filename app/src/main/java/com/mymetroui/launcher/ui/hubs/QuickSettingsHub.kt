package com.mymetroui.launcher.ui.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.R
import com.mymetroui.launcher.ui.actioncenter.ActionCenterActions
import com.mymetroui.launcher.ui.actioncenter.ActionCenterState
import com.mymetroui.launcher.ui.actioncenter.QuickSettingsSection

@Composable
fun ColumnScope.QuickSettingsHub(state: ActionCenterState, actions: ActionCenterActions, onOpenSettings: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    QuickSettingsSection(state, actions)
    Text(
        stringResource(R.string.settings),
        Modifier.padding(top = 24.dp).background(scheme.primary).clickable(onClick = onOpenSettings)
            .padding(horizontal = 20.dp, vertical = 12.dp).testTag("hub-open-settings"),
        color = scheme.onPrimary,
    )
}
