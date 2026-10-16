package com.mymetroui.launcher.ui.home

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.R

fun isDefaultLauncher(context: Context): Boolean {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == context.packageName
}

/** RoleManager dialog on API 29+, the Home settings screen on API 26-28. */
fun defaultLauncherIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roles = context.getSystemService(RoleManager::class.java)
        if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
}

@Composable
fun DefaultLauncherPrompt(isDefault: Boolean, onChoose: () -> Unit, modifier: Modifier = Modifier) {
    if (isDefault) return
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)).padding(16.dp).testTag("default-prompt"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.set_default_title), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Box(
            Modifier.background(MaterialTheme.colorScheme.primary).clickable(onClick = onChoose).padding(12.dp)
                .testTag("default-prompt-choose"),
        ) { Text(stringResource(R.string.set_default_action)) }
    }
}
