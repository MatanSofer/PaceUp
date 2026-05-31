package com.example.paceup.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.paceup.feature.home.LatLng

@SuppressLint("MissingPermission")
@Composable
actual fun LocationEffect(onLocation: (LatLng) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return@LaunchedEffect

        val lm = context.getSystemService(LocationManager::class.java) ?: return@LaunchedEffect
        val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstNotNullOfOrNull { provider ->
                try { lm.getLastKnownLocation(provider) } catch (_: Exception) { null }
            }

        if (location != null) {
            onLocation(LatLng(location.latitude, location.longitude))
        }
    }
}
