package com.mymetroui.launcher.ui.applist

import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.mymetroui.launcher.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.data.AppInfo
import com.mymetroui.launcher.ui.common.DrawableImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    groups: Map<Char, List<AppInfo>>,
    onLaunch: (AppInfo) -> Unit,
    onPin: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
    loadIcon: ((AppInfo) -> Drawable)? = null,
    onOpenSettings: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showJump by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<AppInfo?>(null) }

    val headerIndex = remember(groups) {
        var i = 0
        buildMap { groups.forEach { (c, apps) -> put(c, i); i += 1 + apps.size } }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), state = listState) {
            groups.forEach { (letter, apps) ->
                item(key = "h$letter") {
                    Box(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).size(48.dp)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { showJump = true }
                            .testTag("header-$letter"),
                        contentAlignment = Alignment.Center,
                    ) { Text(letter.toString(), style = MaterialTheme.typography.titleMedium, maxLines = 1) }
                }
                items(apps.size, key = { apps[it].key }) { i ->
                    val app = apps[i]
                    Row(
                        Modifier.fillMaxWidth()
                            .combinedClickable(onClick = { onLaunch(app) }, onLongClick = { menuFor = app })
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (loadIcon != null) {
                            DrawableImage(remember(app.key) { loadIcon(app) }, 40.dp)
                            Box(Modifier.width(16.dp))
                        }
                        Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
item(key = "settings") { Text( stringResource(R.string.settings), Modifier.fillMaxWidth().clickable(onClick = onOpenSettings).padding(16.dp).testTag("settings-row"), style = MaterialTheme.typography.bodyLarge, ) }
        }
        if (showJump) {
            JumpList(
                available = groups.keys,
                onPick = { c ->
                    showJump = false
                    headerIndex[c]?.let { scope.launch { listState.scrollToItem(it) } }
                },
                onDismiss = { showJump = false },
            )
        }
        menuFor?.let { app ->
            AppMenu(
                label = app.label,
                onPin = { menuFor = null; onPin(app) },
                onUninstall = { menuFor = null; onUninstall(app) },
                onDismiss = { menuFor = null },
            )
        }
    }
}
