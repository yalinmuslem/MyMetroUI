package com.mymetroui.launcher.data.notif

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.StatusBarNotification

/** Plain snapshot of a StatusBarNotification so mapping can be tested without framework objects. */
data class RawNotif(
    val key: String,
    val packageName: String,
    val userId: Int,
    val title: CharSequence?,
    val text: CharSequence?,
    val whenMs: Long,
    val flags: Int,
    val visibility: Int,
    val contentIntent: PendingIntent?,
)

data class NotifItem(
    val key: String,
    val packageName: String,
    val userId: Int,
    val title: String,
    val text: String,
    val whenMs: Long,
    val clearable: Boolean,
    val visibility: Int,
    val contentIntent: PendingIntent?,
    val autoCancel: Boolean,
)

/** [userId] must be the same user serial AppRepository uses, so badges match tiles. */
fun StatusBarNotification.toRaw(userId: Int): RawNotif {
    val n = notification
    val extras = n.extras
    return RawNotif(
        key = key,
        packageName = packageName,
        userId = userId,
        title = extras?.getCharSequence(Notification.EXTRA_TITLE),
        text = extras?.getCharSequence(Notification.EXTRA_TEXT),
        whenMs = n.`when`,
        flags = n.flags,
        visibility = n.visibility,
        contentIntent = n.contentIntent,
    )
}

fun mapNotifications(raw: List<RawNotif>, ownPackage: String): List<NotifItem> =
    raw.filter { it.packageName != ownPackage && it.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
        .map {
            NotifItem(
                key = it.key,
                packageName = it.packageName,
                userId = it.userId,
                title = it.title?.toString().orEmpty(),
                text = it.text?.toString().orEmpty(),
                whenMs = it.whenMs,
                clearable = it.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR) == 0,
                visibility = it.visibility,
                contentIntent = it.contentIntent,
                autoCancel = it.flags and Notification.FLAG_AUTO_CANCEL != 0,
            )
        }
        .sortedByDescending { it.whenMs }
