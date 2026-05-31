package com.example.paceup.platform

import androidx.compose.runtime.Composable
import com.example.paceup.feature.home.LatLng

@Composable
actual fun LocationEffect(onLocation: (LatLng) -> Unit) {
    // iOS: implement using CLLocationManager — verify on Mac before PR
    // TODO(paceup): iOS location via CLLocationManager
}
