package com.example.paceup.shared.auth.strava

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens the Strava OAuth URL in a browser tab. */
actual class OAuthBrowserLauncher(private val context: Context) {
    actual fun launch(url: String) {
        // TODO(paceup): upgrade to Chrome Custom Tabs in Task 11.1 (deep links) for a seamless in-app experience
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
