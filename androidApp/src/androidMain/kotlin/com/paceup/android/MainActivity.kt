package com.paceup.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.paceup.App
import com.example.paceup.shared.auth.strava.StravaOAuthCodeStore
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.notifications.NotificationRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val TAG = "MainActivity"

/** Android entry-point activity — hosts the shared Compose Multiplatform [App] composable. */
class MainActivity : ComponentActivity() {

    private val notificationRepository: NotificationRepository by inject()
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        // Fetch the current FCM token and register it. onNewToken() handles future refreshes;
        // this call covers the case where the token already existed before the user logged in.
        registerFcmToken()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle Strava OAuth redirect when app is already running (singleTop resumes)
        handleStravaDeepLink(intent)
    }

    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                AppLogger.w(TAG, "FCM token fetch failed: ${task.exception?.message}")
                return@addOnCompleteListener
            }
            val token = task.result
            AppLogger.d(TAG, "FCM token fetched — registering")
            activityScope.launch { notificationRepository.registerPushToken(token) }
        }
    }

    private fun handleStravaDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme == "paceup" && uri.host == "localhost") {
            val code = uri.getQueryParameter("code") ?: return
            StravaOAuthCodeStore.submitCode(code)
        }
    }
}
