package com.example.paceup.platform

import android.content.Context
import android.content.Intent

/** Opens a transparent Activity that shows the system location permission dialog. */
actual class LocationPermissionRequester(private val context: Context) {
    actual fun request() {
        context.startActivity(
            Intent(context, LocationPermissionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
