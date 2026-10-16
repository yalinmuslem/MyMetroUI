package com.mymetroui.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.mymetroui.launcher.ui.motion.MotionSpec
import com.mymetroui.launcher.ui.motion.TurnstileState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.compose.foundation.layout.statusBarsPadding
import com.mymetroui.launcher.domain.next
import com.mymetroui.launcher.ui.start.StartGrid
import com.mymetroui.launcher.data.live.LiveFace
import com.mymetroui.launcher.data.live.LiveKind
import com.mymetroui.launcher.ui.TileContent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import com.mymetroui.launcher.ui.settings.SettingsScreen
import com.mymetroui.launcher.data.notif.NotifItem
import com.mymetroui.launcher.data.notif.badgeKey
import com.mymetroui.launcher.data.quick.QuickKind
import com.mymetroui.launcher.ui.actioncenter.ActionCenterController
import com.mymetroui.launcher.ui.actioncenter.ActionCenterHost
import com.mymetroui.launcher.ui.actioncenter.rememberActionCenterHostState
import androidx.compose.runtime.rememberCoroutineScope
import com.mymetroui.launcher.data.hub.HubKind
import com.mymetroui.launcher.ui.actioncenter.ActionCenterActions
import com.mymetroui.launcher.ui.hubs.HubScreen
import com.mymetroui.launcher.ui.hubs.HubActions
import com.mymetroui.launcher.ui.hubs.PhotosHub
import com.mymetroui.launcher.ui.hubs.PeopleHub
import com.mymetroui.launcher.ui.hubs.QuickSettingsHub
import com.mymetroui.launcher.ui.welcome.WelcomeScreen
import com.mymetroui.launcher.ui.welcome.welcomeSummary
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
    resumes: Flow<Unit> = emptyFlow(),
    onLaunchApp: (com.mymetroui.launcher.data.AppInfo) -> Unit = {},
    theme: com.mymetroui.launcher.data.ThemeSource,
    onAddLive: (LiveKind) -> Unit = {},
    onLiveTap: (LiveKind, LiveFace?) -> Unit = { _, _ -> },
    loadIcon: (com.mymetroui.launcher.data.AppInfo) -> android.graphics.drawable.Drawable,
    controller: ActionCenterController,
    badgeCounts: Map<String, Int> = emptyMap(),
    onOpenNotification: (NotifItem) -> Unit = {},
    onQuick: (QuickKind) -> Unit = {},
    onRequestWriteSettings: () -> Unit = {},
    onGrantAccess: () -> Unit = {},
    welcomeVisible: Boolean = false,
    onWelcomeDismiss: () -> Unit = {},
    welcomeEnabled: Boolean = true,
    onWelcomeEnabled: (Boolean) -> Unit = {},
    onAddHub: (HubKind) -> Unit = {},
    immersiveEnabled: Boolean = false,
    onImmersiveEnabled: (Boolean) -> Unit = {},
    hubs: HubActions = HubActions(),
) {
    val pager = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val groups by appList.groups.collectAsState()
    val tiles by start.state.collectAsState()
    val themeSettings by theme.settings.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var openHub by remember { mutableStateOf<HubKind?>(null) }
    var editing by remember { mutableStateOf(false) }
    val exitEdit = { editing = false; start.onEditFinished() }

    val turnstile = remember { TurnstileState() }
    val panel = rememberActionCenterHostState(controller)
    val started = LocalLifecycleOwner.current.lifecycle.currentStateAsState().value.isAtLeast(Lifecycle.State.STARTED)

    // Home button: back to Start and fly the tiles in. Resume: swing tiles back in after an app launch.
    LaunchedEffect(homePresses) {
        homePresses.collect {
            launch { turnstile.flyIn() }
            pager.animateScrollToPage(0)
        }
    }
    LaunchedEffect(resumes) { resumes.collect { turnstile.playIn() } }
    BackHandler(enabled = editing, onBack = exitEdit)
    BackHandler(enabled = !editing && pager.currentPage == 1) { scope.launch { pager.animateScrollToPage(0) } }
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = panel.isOpen) { panel.close() } // registered last, so it wins
    BackHandler(enabled = openHub != null) { openHub = null }
    BackHandler(enabled = welcomeVisible, onBack = onWelcomeDismiss)
    val panelState by controller.state.collectAsState()
    val clockFace by remember(start) { start.live.face(LiveKind.CLOCK) }.collectAsState(null)
    val calendarFace by remember(start) { start.live.face(LiveKind.CALENDAR) }.collectAsState(null)
    LaunchedEffect(welcomeVisible) {
        if (welcomeVisible) {
            editing = false; showSettings = false; openHub = null; panel.close()
            controller.refresh()
            pager.scrollToPage(0)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Backdrop is wider than the screen and drifts slower than the pages.
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val bg = MaterialTheme.colorScheme.background
        Box(
            Modifier.requiredWidth(maxWidth * 1.3f).fillMaxHeight()
                .graphicsLayer {
                    val page = pager.currentPage + pager.currentPageOffsetFraction
                    translationX = -page * widthPx * MotionSpec.ParallaxBackgroundFactor
                }
                .background(Brush.horizontalGradient(listOf(bg, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f).compositeOver(bg), bg))),
        )
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                Box(Modifier.fillMaxSize()) {
                    StartGrid(
                        tiles = tiles,
                        editing = editing,
                        onTileTap = { ui, bounds ->
                            if (!editing) (ui.content as? TileContent.Hub)?.kind?.let { openHub = it }
                            val app = ui.app
                            if (app != null && turnstile.progress == 0f) scope.launch {
                                turnstile.playOut()
                                // never leave the tiles swung out if the app could not be started
                                if (!start.launch(app, bounds)) turnstile.playIn()
                            }
                            (ui.content as? TileContent.Live)?.kind?.let { kind ->
                                scope.launch {
                                    val face = withTimeoutOrNull(500) { start.live.face(kind).firstOrNull() }
                                    onLiveTap(kind, face)
                                }
                            }
                        },
                        turnstile = turnstile,
                        liveFace = start.live::face,
                        flipEnabled = !editing && turnstile.progress == 0f && pager.currentPage == 0 && started,
                        onFlipped = start.live::onFlipped,
                        badgeFor = { app -> badgeCounts[badgeKey(app.component.packageName, app.userId)] ?: 0 },
                        onTileLongPress = { editing = true },
                        loadIcon = loadIcon,
                        modifier = Modifier.fillMaxSize().statusBarsPadding(),
                        onMove = start::move,
                        onResize = { id -> tiles.firstOrNull { it.tile.id == id }?.let { start.resize(id, it.tile.size.next()) } },
                        onUnpin = start::unpin,
                        onExitEdit = exitEdit,
                    )
                    Box(
                        Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp).size(44.dp)
                            .border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
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
                    onOpenSettings = { showSettings = true },
                )
            }
        }
        if (showSettings) {
            SettingsScreen(
                settings = themeSettings,
                onAccent = theme::setAccent,
                onMode = theme::setMode,
                onClose = { showSettings = false },
                pinnedLive = tiles.mapNotNull { (it.content as? TileContent.Live)?.kind }.toSet(),
                onAddLive = onAddLive,
                onOpenActionCenter = { showSettings = false; panel.open() },
                pinnedHubs = tiles.mapNotNull { (it.content as? TileContent.Hub)?.kind }.toSet(),
                onAddHub = onAddHub,
                immersiveEnabled = immersiveEnabled,
                onImmersiveEnabled = onImmersiveEnabled,
                welcomeEnabled = welcomeEnabled,
                onWelcomeEnabled = onWelcomeEnabled,
            )
        }
        openHub?.let { kind ->
            HubScreen(kind, onClose = { openHub = null }, scrollable = kind == HubKind.SETTINGS) {
                val photoVersion by (hubs.photoStore?.changes ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0) }).collectAsState()
                when (kind) {
                    HubKind.SETTINGS -> QuickSettingsHub(
                        panelState,
                        ActionCenterActions(
                            onQuick = onQuick, onTorch = controller::setTorch, onBrightness = controller::setBrightness,
                            onRequestWriteSettings = onRequestWriteSettings, onDismiss = {}, onClearAll = {},
                            onOpen = {}, onGrantAccess = onGrantAccess, onClose = {},
                        ),
                        onOpenSettings = { openHub = null; showSettings = true },
                    )
                    HubKind.PHOTOS -> PhotosHub(
                        remember(photoVersion) { hubs.photoStore?.uris().orEmpty() },
                        hubs.loadThumbnail, hubs.onOpenPhoto, hubs.onAddPhotos,
                    )
                    HubKind.PEOPLE -> hubs.contacts?.let { PeopleHub(it, hubs.contactsVersion, hubs.onRequestContacts, hubs.onOpenContact) }
                }
            }
        }
        ActionCenterHost(
            host = panel,
            controller = controller,
            canOpen = !editing && turnstile.progress == 0f && !welcomeVisible,
            onOpenNotification = onOpenNotification,
            onQuick = onQuick,
            onRequestWriteSettings = onRequestWriteSettings,
            onGrantAccess = onGrantAccess,
        )
        if (welcomeVisible) {
            WelcomeScreen(
                clock = clockFace as? LiveFace.Text,
                calendar = calendarFace,
                summary = welcomeSummary(panelState.items, panelState.connected, panelState.deviceLocked),
                onDismiss = { onWelcomeDismiss(); scope.launch { turnstile.flyIn() } },
            )
        }
    }
}
