package com.example.paceup.platform

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Requests notification authorization on iOS via UNUserNotificationCenter.
 * iOS: verify on Mac before PR. The request is fire-and-forget — if already
 * authorized, iOS completes the callback immediately without showing a dialog.
 */
actual class NotificationPermissionRequester {
    actual fun request() {
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or
                    UNAuthorizationOptionBadge or
                    UNAuthorizationOptionSound,
                completionHandler = { _, _ -> }
            )
    }

    // iOS status check is async — returning false here means the explanation screen
    // is always shown on iOS (user taps Allow → requestAuthorization completes silently
    // if already granted). The wasDeclined() flag in NotificationPermissionPrefs handles
    // the "maybe later" skip path.
    // TODO(paceup): implement proper async status check in Task 11.x.
    actual fun isGranted(): Boolean = false
}
