package com.hackfuture.trading.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hackfuture.trading.MainActivity
import com.hackfuture.trading.R
import timber.log.Timber

/**
 * 本地推送通知工具
 * 用于在 FCM 消息之外发送本地通知（如下单结果确认）
 */
object PushNotificationHelper {

    private const val CHANNEL_LOCAL = "local_notifications"
    private const val CHANNEL_LOCAL_NAME = "本地通知"
    private const val NOTIFICATION_GROUP = "com.hackfuture.trading.notifications"

    fun showLocalNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = System.currentTimeMillis().toInt(),
        deepLink: String? = null,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            deepLink?.let { data = android.net.Uri.parse(it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_LOCAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(NOTIFICATION_GROUP)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Timber.w(e, "Notification permission not granted")
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
