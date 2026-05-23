package com.example.paceup.feature.stravaconnect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.shared.pacezone.PaceZone
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import org.jetbrains.compose.resources.stringResource
import com.example.paceup.shared.auth.strava.OAuthBrowserLauncher
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import paceup.composeapp.generated.resources.Res
import paceup.composeapp.generated.resources.strava_avg_pace_label
import paceup.composeapp.generated.resources.strava_connect_button
import paceup.composeapp.generated.resources.strava_connect_data_note
import paceup.composeapp.generated.resources.strava_connect_skip
import paceup.composeapp.generated.resources.strava_connect_subtitle
import paceup.composeapp.generated.resources.strava_connect_title
import paceup.composeapp.generated.resources.strava_connected_no_runs_body
import paceup.composeapp.generated.resources.strava_connected_no_runs_title
import paceup.composeapp.generated.resources.strava_connected_title
import paceup.composeapp.generated.resources.strava_connecting
import paceup.composeapp.generated.resources.strava_continue_button
import paceup.composeapp.generated.resources.strava_pace_zone_label
import paceup.composeapp.generated.resources.strava_retry_button

// ── Colour tokens ────────────────────────────────────────────────────────────
private val BackgroundColor = Color(0xFF0D1B2A)
private val SurfaceColor = Color(0xFF1F2937)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val StravaOrange = Color(0xFFFF5722)
private val SuccessGreen = Color(0xFF10B981)
private val ErrorRed = Color(0xFFEF4444)
private val ElectricBlue = Color(0xFF4FC3F7)

// ── Zone colours (mirrors PaceZone enum) ─────────────────────────────────────
private fun paceZoneColor(zone: PaceZone): Color = when (zone) {
    PaceZone.A -> Color(0xFFA855F7)
    PaceZone.B -> Color(0xFF3B82F6)
    PaceZone.C -> Color(0xFF10B981)
    PaceZone.D -> Color(0xFFF59E0B)
    PaceZone.E -> Color(0xFF6B7280)
}

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun StravaConnectRoot(
    onNavigateToLocationPermission: () -> Unit,
    viewModel: StravaConnectViewModel = koinViewModel(),
    oAuthLauncher: OAuthBrowserLauncher = koinInject()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is StravaConnectEvent.LaunchOAuthUrl -> oAuthLauncher.launch(event.url)
            StravaConnectEvent.NavigateToLocationPermission -> onNavigateToLocationPermission()
        }
    }

    StravaConnectScreen(state = state, onAction = viewModel::onAction)
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun StravaConnectScreen(
    state: StravaConnectState,
    onAction: (StravaConnectAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            when {
                state.isLoading -> LoadingSection()
                state.isConnected -> ConnectedSection(state = state, onAction = onAction)
                state.error != null -> ErrorSection(state = state, onAction = onAction)
                else -> ConnectSection(state = state, onAction = onAction)
            }

            Spacer(Modifier.weight(1f))

            // Skip — only shown on initial connect screen, not after connecting
            if (!state.isConnected && !state.isLoading) {
                TextButton(
                    onClick = { onAction(StravaConnectAction.OnSkipClicked) },
                    modifier = Modifier.testTag("strava_skip_button")
                ) {
                    Text(
                        text = stringResource(Res.string.strava_connect_skip),
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

// ── Connect (initial) ─────────────────────────────────────────────────────────

@Composable
private fun ConnectSection(
    state: StravaConnectState,
    onAction: (StravaConnectAction) -> Unit
) {
    // Strava logo placeholder — S lettermark with orange ring
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(StravaOrange.copy(alpha = 0.12f))
            .border(2.dp, StravaOrange.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "S",
            color = StravaOrange,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black
        )
    }

    Spacer(Modifier.height(24.dp))

    Text(
        text = stringResource(Res.string.strava_connect_title),
        color = TextPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(Res.string.strava_connect_subtitle),
        color = TextMuted,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = stringResource(Res.string.strava_connect_data_note),
        color = TextMuted.copy(alpha = 0.6f),
        fontSize = 12.sp,
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
    )

    Spacer(Modifier.height(40.dp))

    Button(
        onClick = { onAction(StravaConnectAction.OnConnectStravaClicked) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("strava_connect_button"),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = StravaOrange,
            disabledContainerColor = StravaOrange.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = stringResource(Res.string.strava_connect_button),
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingSection() {
    CircularProgressIndicator(
        color = StravaOrange,
        strokeWidth = 3.dp,
        modifier = Modifier
            .size(48.dp)
            .testTag("strava_loading_indicator")
    )
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(Res.string.strava_connecting),
        color = TextMuted,
        fontSize = 15.sp,
        textAlign = TextAlign.Center
    )
}

// ── Connected ─────────────────────────────────────────────────────────────────

@Composable
private fun ConnectedSection(
    state: StravaConnectState,
    onAction: (StravaConnectAction) -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
        exit = fadeOut()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Success icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.12f))
                    .border(2.dp, SuccessGreen.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = SuccessGreen,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))

            if (state.paceZone != null) {
                // Verified: show pace zone
                Text(
                    text = stringResource(Res.string.strava_connected_title),
                    color = SuccessGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(Res.string.strava_pace_zone_label),
                    color = TextMuted,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(8.dp))

                PaceZoneBadge(zone = state.paceZone)

                if (state.avgPaceDisplay != null) {
                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = state.avgPaceDisplay,
                        color = ElectricBlue,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = stringResource(Res.string.strava_avg_pace_label),
                        color = TextMuted,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                // Connected but no qualifying runs
                Text(
                    text = stringResource(Res.string.strava_connected_no_runs_title),
                    color = SuccessGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.strava_connected_no_runs_body),
                    color = TextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )
            }

            Spacer(Modifier.height(36.dp))

            Button(
                onClick = { onAction(StravaConnectAction.OnContinueClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("strava_continue_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
            ) {
                Text(
                    text = stringResource(Res.string.strava_continue_button),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorSection(
    state: StravaConnectState,
    onAction: (StravaConnectAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ErrorRed.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = state.error!!.asString(),
            color = ErrorRed,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = { onAction(StravaConnectAction.OnRetryClicked) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("strava_retry_button"),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = StravaOrange)
    ) {
        Text(
            text = stringResource(Res.string.strava_retry_button),
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Pace Zone Badge ───────────────────────────────────────────────────────────

@Composable
private fun PaceZoneBadge(zone: PaceZone) {
    val zoneColor = paceZoneColor(zone)
    Row(
        modifier = Modifier
            .background(zoneColor.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
            .border(1.dp, zoneColor.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ZONE ${zone.name}",
            color = zoneColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "·",
            color = zoneColor.copy(alpha = 0.5f),
            fontSize = 11.sp
        )
        Text(
            text = zone.displayRange,
            color = zoneColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}
