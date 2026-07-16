package com.hackfuture.trading.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hackfuture.trading.MainActivity
import timber.log.Timber

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "trading_alerts"
        private const val CHANNEL_NAME = "交易提醒"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token: $token")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM message: ${message.messageId}")

        val data = message.data
        val notification = message.notification

        when (data["type"]) {
            "price_alert" -> handlePriceAlert(data, notification)
            "order_filled" -> handleOrderFilled(data, notification)
            "position_liquidation" -> handleLiquidation(data, notification)
            "account_update" -> handleAccountUpdate(data, notification)
            else -> handleDefaultNotification(notification)
        }
    }

    private fun handlePriceAlert(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val symbol = data["symbol"] ?: "Unknown"
        val price = data["price"] ?: "0"
        val target = data["target"] ?: "0"
        showNotification(
            title = "$symbol 价格提醒",
            body = "当前价格 $price，目标价 $target",
            deepLink = "market://ticker?symbol=$symbol",
        )
    }

    private fun handleOrderFilled(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val orderId = data["orderId"] ?: ""
        val side = data["side"] ?: ""
        val qty = data["quantity"] ?: "0"
        showNotification(
            title = "订单已成交",
            body = "$side $qty 已完全成交（订单号: $orderId）",
            deepLink = "trading://order?id=$orderId",
        )
    }

    private fun handleLiquidation(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val symbol = data["symbol"] ?: "Unknown"
        val amount = data["amount"] ?: "0"
        showNotification(
            title = "强平提醒",
            body = "$symbol 持仓被强平，金额: $amount USDT",
            priority = NotificationCompat.PRIORITY_MAX,
            deepLink = "position://list",
        )
    }

    private fun handleAccountUpdate(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        showNotification(
            title = notification?.title ?: "账户更新",
            body = notification?.body ?: "您的账户信息已更新",
        )
    }

    private fun handleDefaultNotification(notification: RemoteMessage.Notification?) {
        if (notification != null) {
            showNotification(
                title = notification.title ?: getString(com.hackfuture.trading.R.string.app_name),
                body = notification.body ?: "",
            )
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        priority: Int = NotificationCompat.PRIORITY_HIGH,
        deepLink: String? = null,
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            deepLink?.let { data = android.net.Uri.parse(it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Timber.w(e, "Notification permission not granted")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "交易提醒、价格预警和强平通知"
                enableVibration(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(ch)
        }
    }

    private fun sendTokenToServer(token: String) {
        Timber.d("Uploading FCM token to server: $token")
        // TODO: 通过 ApiService 上传 token
    }
}
