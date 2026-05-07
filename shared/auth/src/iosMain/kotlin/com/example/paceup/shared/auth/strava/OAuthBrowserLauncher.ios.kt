package com.example.paceup.shared.auth.strava

// iOS: implemented in iosApp/ — verify on Mac before PR
// TODO(paceup): replace with ASWebAuthenticationSession in Task 11.1 (deep links)
actual class OAuthBrowserLauncher {
    actual fun launch(url: String) {
        // Stub — ASWebAuthenticationSession wired in Task 11.1
    }
}
