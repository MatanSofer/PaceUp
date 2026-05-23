package com.example.paceup.platform

/**
 * Launches the system notification permission dialog and checks current status.
 * Implemented per-platform via expect/actual.
 * iOS: verified on Mac before PR.
 */
expect class NotificationPermissionRequester {
    /** Opens the system permission dialog (Android 13+ only; no-op on older versions). */
    fun request()

    /** Returns true if notifications are enabled (Android: all versions; iOS: authorized). */
    fun isGranted(): Boolean
}
