package com.example.paceup.platform

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Transparent single-purpose Activity that shows the system location permission dialog.
 * Finishes immediately after the user responds. Navigation proceeds on the caller's side
 * regardless of the result — location is optional for run discovery.
 */
class LocationPermissionActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Result ignored — caller navigates forward whether granted or denied
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
