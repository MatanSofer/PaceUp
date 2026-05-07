package com.example.paceup.shared.auth.strava

/**
 * Platform-specific OAuth browser launcher.
 * Android: Chrome Custom Tabs (see androidMain actual).
 * iOS: ASWebAuthenticationSession (see iosMain actual — verify on Mac before PR).
 */
expect class OAuthBrowserLauncher {
    fun launch(url: String)
}
