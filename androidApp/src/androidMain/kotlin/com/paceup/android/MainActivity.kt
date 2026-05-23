package com.paceup.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.paceup.App
import com.example.paceup.shared.auth.strava.StravaOAuthCodeStore

/** Android entry-point activity — hosts the shared Compose Multiplatform [App] composable. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App(
                appVersion = BuildConfig.VERSION_NAME,
                onOpenAppStore = {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                    )
                }
            )
        }
        // Handle Strava OAuth redirect on cold-start (app was not in foreground)
        handleStravaDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle Strava OAuth redirect when app is already running (singleTop resumes)
        handleStravaDeepLink(intent)
    }

    private fun handleStravaDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme == "paceup" && uri.host == "localhost") {
            val code = uri.getQueryParameter("code") ?: return
            StravaOAuthCodeStore.submitCode(code)
        }
    }
}
