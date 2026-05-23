package com.example.paceup.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.paceup.feature.home.LatLng
import com.example.paceup.shared.runmatching.domain.Run

// iOS: MapKit implementation via UIViewRepresentable — verify on Mac before PR
// TODO(paceup): implement MapKit map with run pins for iOS in Task 3.2 iOS parity
@Composable
actual fun PaceUpMap(
    runs: List<Run>,
    selectedRunId: String?,
    userLocation: LatLng?,
    center: LatLng,
    onPinClick: (Run) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Map (iOS — implement MapKit)",
            color = Color(0xFF4B5563),
        )
    }
}
