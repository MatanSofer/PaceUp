package com.example.paceup.platform

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi

/**
 * Transparent single-purpose Activity that shows the POST_NOTIFICATIONS system dialog (API 33+).
 * Finishes immediately after the user responds. Navigation proceeds regardless of result.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class NotificationPermissionActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
