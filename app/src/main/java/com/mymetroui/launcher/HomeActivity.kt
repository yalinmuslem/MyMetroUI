package com.mymetroui.launcher

import android.content.Intent
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

/** Carries "Home pressed" events from onNewIntent to the Compose tree. */
class HomeEvents : ViewModel() {
    private val _homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homePresses: SharedFlow<Unit> = _homePresses
    fun homePressed() { _homePresses.tryEmit(Unit) }

    private val _resumes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resumes: SharedFlow<Unit> = _resumes
    fun resumed() { _resumes.tryEmit(Unit) }
}

class HomeActivity : ComponentActivity() {
    private val factory by lazy {
        val app = application as LauncherApp
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                StartViewModel::class.java -> StartViewModel(app.tileRepository, app.appRepository)
                AppListViewModel::class.java -> AppListViewModel(app.tileRepository, app.appRepository)
                else -> HomeEvents()
            } as T
        }
    }
    private val start: StartViewModel by viewModels { factory }
    private val appList: AppListViewModel by viewModels { factory }
    private val events: HomeEvents by viewModels { factory }
    private var isDefault by mutableStateOf(true)
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

    private fun applySystemBars(mode: ThemeMode) {
        val clear = android.graphics.Color.TRANSPARENT
        val style = if (mode == ThemeMode.DARK) SystemBarStyle.dark(clear) else SystemBarStyle.light(clear, clear)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun onResume() {
        super.onResume()
        isDefault = isDefaultLauncher(this)
        events.resumed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && Intent.CATEGORY_HOME in (intent.categories ?: emptySet())) {
            events.homePressed()
        }
    }
}
