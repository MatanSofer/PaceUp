package com.example.paceup.shared.auth.strava

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * Opens the Strava OAuth URL in Safari on iOS.
 * iOS: implemented here — verify on Mac before PR.
 *
 * TODO(paceup): upgrade to SFSafariViewController (in-app sheet with Done button)
 *   in Task 11.1 for parity with Android Chrome Custom Tabs.
 */
actual class OAuthBrowserLauncher {
    actual fun launch(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(
            url = nsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = null
        )
    }
}
