package com.mymetroui.launcher.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.provider.MediaStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class AppRepository(private val context: Context) : AppSource {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    override val apps: Flow<List<AppInfo>> = callbackFlow {
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageAdded(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageChanged(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
        }
        // this block runs on Dispatchers.IO, which has no Looper; the callbacks only trySend, so the main looper is fine
        launcherApps.registerCallback(cb, Handler(Looper.getMainLooper()))
        trySend(Unit)
        awaitClose { launcherApps.unregisterCallback(cb) }
    }.conflate().let { events ->
        kotlinx.coroutines.flow.flow { events.collect { emit(query()) } }
    }.flowOn(Dispatchers.IO)

    private fun query(): List<AppInfo> =
        userManager.userProfiles.flatMap { user ->
            val userId = userManager.getSerialNumberForUser(user).toInt()
            launcherApps.getActivityList(null, user).map {
                AppInfo(makeKey(it.componentName, userId), it.label.toString(), userId, it.componentName, user)
            }
        }.sortedBy { it.label.lowercase() }

    override fun launch(app: AppInfo, sourceBounds: Rect?): Boolean = try {
        launcherApps.startMainActivity(app.component, app.user, sourceBounds, null)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    override fun loadIcon(app: AppInfo, density: Int): Drawable =
        launcherApps.getActivityList(app.component.packageName, app.user)
            .firstOrNull { it.componentName == app.component }?.getIcon(density)
            ?: context.packageManager.defaultActivityIcon

    /** Package name of the system default handler for a [defaultTileKeys] category. */
    override fun resolveDefault(category: String): String? {
        val intent = when (category) {
            "phone" -> Intent(Intent.ACTION_DIAL)
            "messaging" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
            "browser" -> Intent(Intent.ACTION_VIEW, android.net.Uri.parse("http://"))
            "camera" -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            "gallery" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY)
            else -> return null
        }
        return context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName?.takeIf { it != "android" }
    }

    override fun uninstall(app: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:${app.component.packageName}"))
            .putExtra(Intent.EXTRA_USER, app.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {}
    }
}
