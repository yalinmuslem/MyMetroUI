package com.mymetroui.launcher

import android.app.Application
import com.mymetroui.launcher.data.AppRepository
import com.mymetroui.launcher.data.ThemeRepository
import com.mymetroui.launcher.data.TileRepository
import com.mymetroui.launcher.data.db.AppDatabase

/** Process-wide singletons shared by every HomeActivity instance. */
class LauncherApp : Application() {
    val appRepository by lazy { AppRepository(this) }
    val tileRepository by lazy { TileRepository(AppDatabase.create(this).tileDao()) }
    val themeRepository by lazy { ThemeRepository(getSharedPreferences("theme", MODE_PRIVATE)) }
}
