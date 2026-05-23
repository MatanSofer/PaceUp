package com.example.paceup.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** Opens a transparent Activity that shows the system location permission dialog. */
actual class LocationPermissionRequester(private val context: Context) {
    actual fun request() {
        context.startActivity(
            Intent(context, LocationPermissionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    actual fun isGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
