package com.mymetroui.launcher.data.notif

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

fun isNotificationAccessGranted(context: Context): Boolean =
    context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

fun notificationAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
