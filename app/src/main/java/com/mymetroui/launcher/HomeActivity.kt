package com.mymetroui.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as LauncherApp).appRepository
        setContent {
            MyMetroUITheme {
                HomeScreen(
                    start = start,
                    appList = appList,
                    homePresses = events.homePresses,
                    onLaunchApp = { repo.launch(it, null) },
                    loadIcon = { repo.loadIcon(it, resources.displayMetrics.densityDpi) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && Intent.CATEGORY_HOME in (intent.categories ?: emptySet())) {
            events.homePressed()
        }
    }
}
