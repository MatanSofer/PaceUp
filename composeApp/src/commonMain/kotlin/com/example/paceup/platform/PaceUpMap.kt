package com.example.paceup.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.paceup.feature.home.LatLng
import com.example.paceup.shared.runmatching.domain.Run

/**
 * Platform-specific map composable.
 * Android actual: Google Maps Compose with dark theme and zone-colored pins.
 * iOS actual: MapKit via UIKit interop — verify on Mac before PR.
 */
@Composable
expect fun PaceUpMap(
    runs: List<Run>,
    selectedRunId: String?,
    userLocation: LatLng?,
    center: LatLng,
    onPinClick: (Run) -> Unit,
    modifier: Modifier = Modifier,
)
