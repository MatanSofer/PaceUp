package com.example.paceup.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paceup.feature.home.LatLng
import com.example.paceup.feature.home.paceZone
import com.example.paceup.feature.home.color
import com.example.paceup.shared.runmatching.domain.Run
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private val darkMapStyle = """
[
  {"featureType":"all","elementType":"geometry","stylers":[{"color":"#0d1b2a"}]},
  {"featureType":"all","elementType":"labels.text.fill","stylers":[{"color":"#9ca3af"}]},
  {"featureType":"all","elementType":"labels.text.stroke","stylers":[{"color":"#0d1b2a"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#1f2937"}]},
  {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#374151"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#374151"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#111827"}]},
  {"featureType":"poi","stylers":[{"visibility":"off"}]},
  {"featureType":"transit","stylers":[{"visibility":"off"}]},
  {"featureType":"landscape","elementType":"geometry","stylers":[{"color":"#131f2e"}]}
]
""".trimIndent()

@Composable
actual fun PaceUpMap(
    runs: List<Run>,
    selectedRunId: String?,
    userLocation: LatLng?,
    center: LatLng,
    onPinClick: (Run) -> Unit,
    modifier: Modifier,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            com.google.android.gms.maps.model.LatLng(center.lat, center.lng),
            13f
        )
    }

    LaunchedEffect(center) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLng(
                com.google.android.gms.maps.model.LatLng(center.lat, center.lng)
            )
        )
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapStyleOptions = MapStyleOptions(darkMapStyle),
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false,
        ),
    ) {
        runs.forEach { run ->
            val zone = run.paceZone()
            val zoneColor = zone.color()
            val isSelected = run.id == selectedRunId

            MarkerComposable(
                state = MarkerState(
                    position = com.google.android.gms.maps.model.LatLng(
                        run.meetingLat, run.meetingLng
                    )
                ),
                onClick = { onPinClick(run); true },
            ) {
                RunMapPin(
                    label = zone.name,
                    color = zoneColor,
                    isSelected = isSelected,
                )
            }
        }

        userLocation?.let {
            MarkerComposable(
                state = MarkerState(
                    position = com.google.android.gms.maps.model.LatLng(it.lat, it.lng)
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A73E8))
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

/** Teardrop-shaped map pin showing pace zone. */
@Composable
private fun RunMapPin(label: String, color: Color, isSelected: Boolean) {
    val size = if (isSelected) 52.dp else 44.dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp, bottomEnd = 50.dp, bottomStart = 4.dp))
            .background(color)
            .then(
                if (isSelected) Modifier.border(2.dp, Color.White, RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp, bottomEnd = 50.dp, bottomStart = 4.dp))
                else Modifier
            )
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
