package com.mymetroui.launcher

import android.app.Application
import android.net.Uri
import com.mymetroui.launcher.data.live.CalendarSource
import com.mymetroui.launcher.data.live.ClockSource
import com.mymetroui.launcher.data.live.CompositeLiveSource
import com.mymetroui.launcher.data.live.ContentResolverCalendarQuery
import com.mymetroui.launcher.data.live.LiveTileSource
import com.mymetroui.launcher.data.live.PhotoStore
import com.mymetroui.launcher.data.live.PhotosSource
import com.mymetroui.launcher.data.AppRepository
import com.mymetroui.launcher.data.ThemeRepository
import com.mymetroui.launcher.data.TileRepository
import com.mymetroui.launcher.data.db.AppDatabase

/** Process-wide singletons shared by every HomeActivity instance. */
class LauncherApp : Application() {
    val appRepository by lazy { AppRepository(this) }
    val tileRepository by lazy { TileRepository(AppDatabase.create(this).tileDao()) }
    val themeRepository by lazy { ThemeRepository(getSharedPreferences("theme", MODE_PRIVATE)) }
    val photoStore by lazy { PhotoStore(getSharedPreferences("photos", MODE_PRIVATE)) }
    val liveSource: LiveTileSource by lazy {
        CompositeLiveSource(
            ClockSource(),
            CalendarSource(ContentResolverCalendarQuery(this)),
            PhotosSource(photoStore) { uri -> contentResolver.openInputStream(Uri.parse(uri))?.use { true } ?: false },
        )
    }
}
