package com.example.paceup.platform

import platform.Foundation.NSUserDefaults

private const val KEY_NOTIF_DECLINED = "notif_permission_declined"

actual class NotificationPermissionPrefs {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun wasDeclined(): Boolean = defaults.boolForKey(KEY_NOTIF_DECLINED)

    actual fun markDeclined() = defaults.setBool(true, KEY_NOTIF_DECLINED)
}
