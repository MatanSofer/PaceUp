package com.example.paceup.platform

/**
 * Handles the system location permission dialog and status check.
 * Implemented per-platform via expect/actual.
 * iOS: verified on Mac before PR.
 */
expect class LocationPermissionRequester {
    /** Opens the system permission dialog. */
    fun request()

    /** Returns true if ACCESS_COARSE_LOCATION (Android) or WhenInUse (iOS) is already granted. */
    fun isGranted(): Boolean
}
