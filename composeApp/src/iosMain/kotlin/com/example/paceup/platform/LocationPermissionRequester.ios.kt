package com.example.paceup.platform

import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined

/**
 * Requests "when in use" location authorization on iOS via CLLocationManager.
 * iOS: verify on Mac before PR. Requires NSLocationWhenInUseUsageDescription in Info.plist.
 * TODO(paceup): retain manager as a property to prevent premature deallocation (Task 11.x).
 */
actual class LocationPermissionRequester {
    actual fun request() {
        val manager = CLLocationManager()
        if (manager.authorizationStatus == kCLAuthorizationStatusNotDetermined) {
            manager.requestWhenInUseAuthorization()
        }
    }
}
