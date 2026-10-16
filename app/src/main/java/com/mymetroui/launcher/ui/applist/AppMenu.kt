package com.mymetroui.launcher.ui.applist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.R

@Composable
fun AppMenu(label: String, onPin: () -> Unit, onUninstall: () -> Unit, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(24.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.pin_to_start), Modifier.clickable(onClick = onPin).padding(top = 16.dp, bottom = 8.dp))
            Text(stringResource(R.string.uninstall), Modifier.clickable(onClick = onUninstall).padding(vertical = 8.dp))
        }
    }
}
