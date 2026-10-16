package com.mymetroui.launcher.data.notif

import android.os.UserManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mymetroui.launcher.LauncherApp

class NotificationListener : NotificationListenerService() {
    private val repo get() = (application as LauncherApp).notificationRepository

    private val controller = object : NotificationController {
        override fun cancel(key: String) = cancelNotification(key)
        override fun cancelAll(keys: List<String>) {
            if (keys.isNotEmpty()) cancelNotifications(keys.toTypedArray())
        }
    }

    override fun onListenerConnected() = repo.onConnected(snapshot(), packageName, controller)
    override fun onListenerDisconnected() = repo.onDisconnected()
    override fun onNotificationPosted(sbn: StatusBarNotification) = repo.onChanged(snapshot(), packageName)
    override fun onNotificationRemoved(sbn: StatusBarNotification) = repo.onChanged(snapshot(), packageName)

    private fun snapshot(): List<RawNotif> = try {
        val users = getSystemService(UserManager::class.java)
        activeNotifications.orEmpty().map { it.toRaw(users.getSerialNumberForUser(it.user).toInt()) }
    } catch (_: SecurityException) {
        emptyList()
    }
}
