package com.example.paceup.shared.auth.strava

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Opens the Strava OAuth URL in a Chrome Custom Tab.
 * Custom Tabs slide up over the app and include a close (X) button,
 * so users can always return to PaceUp — even if Strava shows an error.
 */
actual class OAuthBrowserLauncher(private val context: Context) {

    actual fun launch(url: String) {
        val darkNavy = android.graphics.Color.parseColor("#0D1B2A")
        val colorParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(darkNavy)
            .build()

        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(colorParams)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
            .build()
        // Application context requires FLAG_ACTIVITY_NEW_TASK to start an Activity
        customTab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customTab.launchUrl(context, Uri.parse(url))
    }
}
