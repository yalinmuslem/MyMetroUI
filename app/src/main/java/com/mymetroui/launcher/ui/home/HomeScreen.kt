package com.mymetroui.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.ui.AppListViewModel
import com.mymetroui.launcher.ui.StartViewModel
import com.mymetroui.launcher.ui.applist.AppListScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    start: StartViewModel,
    appList: AppListViewModel,
    homePresses: Flow<Unit> = emptyFlow(),
    startPage: @Composable () -> Unit = {},
    onLaunchApp: (com.mymetroui.launcher.data.AppInfo) -> Unit = {},
    loadIcon: ((com.mymetroui.launcher.data.AppInfo) -> android.graphics.drawable.Drawable)? = null,
) {
    val pager = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val groups by appList.groups.collectAsState()

    LaunchedEffect(homePresses) { homePresses.collect { pager.animateScrollToPage(0) } }
    BackHandler(enabled = pager.currentPage == 1) { scope.launch { pager.animateScrollToPage(0) } }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                Box(Modifier.fillMaxSize()) {
                    startPage()
                    Box(
                        Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp).size(44.dp)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { scope.launch { pager.animateScrollToPage(1) } }
                            .testTag("to-app-list"),
                        contentAlignment = Alignment.Center,
                    ) { Text("→") }
                }
            } else {
                AppListScreen(
                    groups = groups,
                    onLaunch = onLaunchApp,
                    onPin = { appList.pin(it.key); scope.launch { pager.animateScrollToPage(0) } },
                    onUninstall = appList::uninstall,
                    loadIcon = loadIcon,
                )
            }
        }
    }
}
