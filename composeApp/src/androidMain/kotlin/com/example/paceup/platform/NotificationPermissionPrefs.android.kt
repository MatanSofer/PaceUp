package com.example.paceup.platform

import android.content.Context

private const val PREFS_NAME = "paceup_prefs"
private const val KEY_NOTIF_DECLINED = "notif_permission_declined"

actual class NotificationPermissionPrefs(private val context: Context) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun wasDeclined(): Boolean = prefs.getBoolean(KEY_NOTIF_DECLINED, false)

    actual fun markDeclined() = prefs.edit().putBoolean(KEY_NOTIF_DECLINED, true).apply()
}
