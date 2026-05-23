package com.example.paceup.feature.notificationpermission

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.platform.NotificationPermissionRequester
import com.example.paceup.ui.ObserveAsEvents
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import paceup.composeapp.generated.resources.Res
import paceup.composeapp.generated.resources.notification_maybe_later_button
import paceup.composeapp.generated.resources.notification_subtitle
import paceup.composeapp.generated.resources.notification_title
import paceup.composeapp.generated.resources.notification_turn_on_button
import paceup.composeapp.generated.resources.notification_why_note

private val BackgroundColor = Color(0xFF0D1B2A)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)
private val AccentAmber = Color(0xFFF59E0B)

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun NotificationPermissionRoot(
    onNavigateToProfileSetup: () -> Unit,
    viewModel: NotificationPermissionViewModel = koinViewModel(),
    permissionRequester: NotificationPermissionRequester = koinInject()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            NotificationPermissionEvent.RequestPermission -> permissionRequester.request()
            NotificationPermissionEvent.NavigateToProfileSetup -> onNavigateToProfileSetup()
        }
    }

    NotificationPermissionScreen(state = state, onAction = viewModel::onAction)
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun NotificationPermissionScreen(
    state: NotificationPermissionState,
    onAction: (NotificationPermissionAction) -> Unit
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

            // Bell icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AccentAmber.copy(alpha = 0.12f))
                    .border(2.dp, AccentAmber.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔔",
                    fontSize = 36.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.notification_title),
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.notification_subtitle),
                color = TextMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.notification_why_note),
                color = TextMuted.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = { onAction(NotificationPermissionAction.OnTurnOnClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("notification_turn_on_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text = stringResource(Res.string.notification_turn_on_button),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = { onAction(NotificationPermissionAction.OnMaybeLaterClicked) },
                modifier = Modifier.testTag("notification_maybe_later_button")
            ) {
                Text(
                    text = stringResource(Res.string.notification_maybe_later_button),
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
