package com.paceup.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.paceup.App

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
    }
}
