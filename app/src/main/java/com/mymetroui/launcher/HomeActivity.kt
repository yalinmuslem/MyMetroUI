package com.mymetroui.launcher

import android.Manifest
import android.app.PendingIntent
import com.mymetroui.launcher.data.notif.NotifItem
import com.mymetroui.launcher.data.notif.badgeCounts
import com.mymetroui.launcher.data.notif.notificationAccessIntent
import com.mymetroui.launcher.data.quick.quickIntent
import com.mymetroui.launcher.data.quick.writeSettingsIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.mymetroui.launcher.data.live.LiveFace
import com.mymetroui.launcher.data.live.LiveKind
import com.mymetroui.launcher.data.live.LiveTap
import com.mymetroui.launcher.data.live.LiveTileAdder
import com.mymetroui.launcher.data.live.liveOpenIntent
import com.mymetroui.launcher.data.live.liveTap
import com.mymetroui.launcher.ui.TileContent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.mymetroui.launcher.data.ThemeMode
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mymetroui.launcher.ui.home.DefaultLauncherPrompt
import com.mymetroui.launcher.ui.home.defaultLauncherIntent
import com.mymetroui.launcher.ui.home.isDefaultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mymetroui.launcher.ui.AppListViewModel
import com.mymetroui.launcher.ui.StartViewModel
import com.mymetroui.launcher.ui.home.HomeScreen
import com.mymetroui.launcher.ui.theme.MyMetroUITheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.os.PowerManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.mymetroui.launcher.data.hub.ContentResolverContactsQuery
import com.mymetroui.launcher.data.hub.HubTileAdder
import com.mymetroui.launcher.ui.hubs.HubActions
import com.mymetroui.launcher.ui.hubs.decodeThumbnail

/** Carries "Home pressed" events from onNewIntent to the Compose tree. */
class HomeEvents : ViewModel() {
    private val _homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homePresses: SharedFlow<Unit> = _homePresses
    fun homePressed() { _homePresses.tryEmit(Unit) }

    private val _resumes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resumes: SharedFlow<Unit> = _resumes
    fun resumed() { _resumes.tryEmit(Unit) }

    private val _welcomeVisible = MutableStateFlow(false)
    val welcomeVisible: StateFlow<Boolean> = _welcomeVisible
    fun showWelcome() { _welcomeVisible.value = true }
    fun dismissWelcome() { _welcomeVisible.value = false }
}

class HomeActivity : ComponentActivity() {
    private val factory by lazy {
        val app = application as LauncherApp
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                StartViewModel::class.java -> StartViewModel(app.tileRepository, app.appRepository, app.liveSource)
                AppListViewModel::class.java -> AppListViewModel(app.tileRepository, app.appRepository)
                else -> HomeEvents()
            } as T
        }
    }
    private val start: StartViewModel by viewModels { factory }
    private val appList: AppListViewModel by viewModels { factory }
    private val events: HomeEvents by viewModels { factory }
    private var isDefault by mutableStateOf(true)
    private var calendarResult: ((Boolean) -> Unit)? = null
    private var photoResult: ((List<String>) -> Unit)? = null
    private var contactsResult: ((Boolean) -> Unit)? = null
    private var contactsVersion by mutableIntStateOf(0)
    private val contactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        contactsResult?.invoke(granted)
        contactsResult = null
        contactsVersion++
    }
    private val hubAdder by lazy {
        HubTileAdder(
            pinned = { start.state.value.mapNotNull { (it.content as? TileContent.Hub)?.kind }.toSet() },
            requestContacts = { cb ->
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) cb(true)
                else { contactsResult = cb; contactsPermission.launch(Manifest.permission.READ_CONTACTS) }
            },
            pin = start::addHub,
        )
    }
    private val hubActions by lazy {
        val app = application as LauncherApp
        HubActions(
            photoStore = app.photoStore,
            loadThumbnail = { uri -> decodeThumbnail({ contentResolver.openInputStream(Uri.parse(uri)) }, uri) },
            onOpenPhoto = { uri ->
                launchSafely(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            },
            onAddPhotos = {
                photoResult = { uris -> if (uris.isNotEmpty()) app.photoStore.add(uris) }
                photoPicker.launch(arrayOf("image/*"))
            },
            contacts = ContentResolverContactsQuery(this),
            onRequestContacts = { contactsPermission.launch(Manifest.permission.READ_CONTACTS) },
            onOpenContact = { c ->
                launchSafely(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(c.id, c.lookupKey)))
            },
        )
    }
    private val calendarPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        calendarResult?.invoke(granted)
        calendarResult = null
    }
    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        // keep read access across reboots; a URI that refuses the grant is simply not used
        val kept = uris.filter { runCatching { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }.isSuccess }
        photoResult?.invoke(kept.map { it.toString() })
        photoResult = null
    }
    private val adder by lazy {
        val app = application as LauncherApp
        LiveTileAdder(
            pinned = { start.state.value.mapNotNull { (it.content as? TileContent.Live)?.kind }.toSet() },
            requestCalendar = { cb ->
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) cb(true)
                else { calendarResult = cb; calendarPermission.launch(Manifest.permission.READ_CALENDAR) }
            },
            pickPhotos = { cb -> photoResult = cb; photoPicker.launch(arrayOf("image/*")) },
            store = app.photoStore,
            pin = start::addLive,
        )
    }

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = isDefaultLauncher(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LauncherApp
        val repo = app.appRepository
        setContent {
            val themeSettings by app.themeRepository.settings.collectAsState()
            val notifications by app.notificationRepository.items.collectAsState()
            val welcomeVisible by events.welcomeVisible.collectAsState()
            val welcomeEnabled by app.welcomePrefs.enabled.collectAsState()
            LaunchedEffect(themeSettings.mode) { applySystemBars(themeSettings.mode) }
            MyMetroUITheme(themeSettings) {
                Box(Modifier.fillMaxSize()) {
                    HomeScreen(
                        start = start,
                        appList = appList,
                        homePresses = events.homePresses,
                        resumes = events.resumes,
                        onLaunchApp = { repo.launch(it, null) },
                        loadIcon = { repo.loadIcon(it, resources.displayMetrics.densityDpi) },
                        theme = app.themeRepository,
                        onAddLive = adder::add,
                        onLiveTap = ::onLiveTap,
                        controller = app.actionCenter,
                        badgeCounts = badgeCounts(notifications),
                        onOpenNotification = { openNotification(app, it) },
                        onQuick = { launchSafely(quickIntent(it)) },
                        onRequestWriteSettings = { launchSafely(writeSettingsIntent(this@HomeActivity)) },
                        onGrantAccess = { launchSafely(notificationAccessIntent()) },
                        welcomeVisible = welcomeVisible,
                        onWelcomeDismiss = events::dismissWelcome,
                        welcomeEnabled = welcomeEnabled,
                        onWelcomeEnabled = app.welcomePrefs::setEnabled,
                        onAddHub = hubAdder::add,
                        hubs = hubActions.copy(contactsVersion = contactsVersion),
                    )
                    DefaultLauncherPrompt(
                        isDefault = isDefault,
                        onChoose = { roleRequest.launch(defaultLauncherIntent(this@HomeActivity)) },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }

    private fun launchSafely(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun openNotification(app: LauncherApp, item: NotifItem) {
        try {
            item.contentIntent?.send()
        } catch (_: PendingIntent.CanceledException) {
            return // the notification is stale; leave it alone
        }
        if (item.autoCancel) app.notificationRepository.dismiss(item.key)
    }

    private fun onLiveTap(kind: LiveKind, face: LiveFace?) {
        when (val tap = liveTap(kind, face)) {
            LiveTap.RequestCalendar -> calendarPermission.launch(Manifest.permission.READ_CALENDAR)
            is LiveTap.Open -> try {
                startActivity(liveOpenIntent(tap.kind).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
            }
        }
    }

    private fun applySystemBars(mode: ThemeMode) {
        val clear = android.graphics.Color.TRANSPARENT
        val style = if (mode == ThemeMode.DARK) SystemBarStyle.dark(clear) else SystemBarStyle.light(clear, clear)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun onResume() {
        super.onResume()
        isDefault = isDefaultLauncher(this)
        events.resumed()
        if ((application as LauncherApp).welcomeGate.onResume()) events.showWelcome()
    }

    override fun onPause() {
        super.onPause()
        val interactive = runCatching { getSystemService(PowerManager::class.java).isInteractive }.getOrDefault(true)
        (application as LauncherApp).welcomeGate.onPause(interactive)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && Intent.CATEGORY_HOME in (intent.categories ?: emptySet())) {
            events.homePressed()
        }
    }
}
