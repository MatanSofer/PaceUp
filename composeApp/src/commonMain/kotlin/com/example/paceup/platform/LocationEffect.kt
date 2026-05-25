package com.example.paceup.platform

import androidx.compose.runtime.Composable
import com.example.paceup.feature.home.LatLng

/**
 * One-shot platform effect that emits the device's current location once when composed.
 * If location permission is not granted or no cached location exists, [onLocation] is never called.
 */
@Composable
expect fun LocationEffect(onLocation: (LatLng) -> Unit)
