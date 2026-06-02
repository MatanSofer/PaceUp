package com.paceup.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.notifications.NotificationRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val TAG = "PaceUpFcmService"
private const val CHANNEL_ID = "paceup_default"

/**
 * Receives FCM push notifications.
 * - [onNewToken]: saves the refreshed token to Supabase so the dispatcher can reach this device.
 * - [onMessageReceived]: shows a system notification when the app is in the foreground.
 */
class PaceUpFirebaseMessagingService : FirebaseMessagingService() {

    private val notificationRepository: NotificationRepository by inject()

    // Scoped to service lifetime; cancelled when service is destroyed
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AppLogger.i(TAG, "FCM token refreshed — registering with Supabase")
        serviceScope.launch {
            notificationRepository.registerPushToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        AppLogger.d(TAG, "foreground message received type=${message.data["type"]}")

        // FCM handles display automatically when app is in background.
        // We only need to post manually when the app is in the foreground.
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return

        showNotification(title, body, message.data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure the default channel exists (required on API 26+)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PaceUp",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            manager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data["run_id"]?.let { putExtra("run_id", it) }
            data["user_id"]?.let { putExtra("user_id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
