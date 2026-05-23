package com.example.paceup.platform

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/** Handles POST_NOTIFICATIONS runtime permission (Android 13+). */
actual class NotificationPermissionRequester(private val context: Context) {
    actual fun request() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.startActivity(
                Intent(context, NotificationPermissionActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        // Below API 33: notifications are on by default; no dialog needed
    }

    actual fun isGranted(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
