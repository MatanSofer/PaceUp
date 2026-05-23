package com.example.paceup.platform

/**
 * Launches the system location permission dialog.
 * Implemented per-platform via expect/actual.
 * iOS: verified on Mac before PR.
 */
expect class LocationPermissionRequester {
    fun request()
}
