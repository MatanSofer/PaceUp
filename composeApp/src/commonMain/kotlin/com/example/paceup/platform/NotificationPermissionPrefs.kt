package com.example.paceup.platform

/**
 * Persists the user's "Maybe later" choice for notification permission.
 * Once declined, the pre-permission screen is skipped on subsequent logins.
 * Implemented per-platform via expect/actual (SharedPreferences / NSUserDefaults).
 */
expect class NotificationPermissionPrefs {
    fun wasDeclined(): Boolean
    fun markDeclined()
}
