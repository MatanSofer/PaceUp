package com.example.paceup.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paceup.ui.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

private val BackgroundColor = Color(0xFF0D1B2A)
private val SurfaceColor    = Color(0xFF1F2937)
private val SurfaceElevated = Color(0xFF374151)
private val DividerColor    = Color(0xFF374151)
private val TextPrimary     = Color(0xFFF9FAFB)
private val TextMuted       = Color(0xFF9CA3AF)

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun SettingsRoot(
    onNavigateBack: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToApp: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.NavigateBack          -> onNavigateBack()
            SettingsEvent.NavigateToAccount     -> onNavigateToAccount()
            SettingsEvent.NavigateToNotifications -> onNavigateToNotifications()
            SettingsEvent.NavigateToPrivacy     -> onNavigateToPrivacy()
            SettingsEvent.NavigateToApp         -> onNavigateToApp()
        }
    }

    SettingsScreen(onAction = viewModel::onAction)
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(onAction: (SettingsAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = "←",
                color = TextPrimary,
                fontSize = 20.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAction(SettingsAction.OnBackClick) }
                    .padding(4.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Settings",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        Spacer(Modifier.height(24.dp))

        // ── Section rows ─────────────────────────────────────────────────────
        SettingsSection(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            SettingsRow(
                icon = "👤",
                title = "Account",
                subtitle = "Profile, email, connected apps",
                onClick = { onAction(SettingsAction.OnAccountClick) },
                isFirst = true,
                isLast = false,
            )
            HorizontalDivider(
                color = DividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 56.dp),
            )
            SettingsRow(
                icon = "🔔",
                title = "Notifications",
                subtitle = "Run reminders, rivals, ratings",
                onClick = { onAction(SettingsAction.OnNotificationsClick) },
                isFirst = false,
                isLast = false,
            )
            HorizontalDivider(
                color = DividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 56.dp),
            )
            SettingsRow(
                icon = "🔒",
                title = "Privacy",
                subtitle = "Visibility, blocked users",
                onClick = { onAction(SettingsAction.OnPrivacyClick) },
                isFirst = false,
                isLast = false,
            )
            HorizontalDivider(
                color = DividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 56.dp),
            )
            SettingsRow(
                icon = "⚙",
                title = "App",
                subtitle = "Language, units, map style",
                onClick = { onAction(SettingsAction.OnAppClick) },
                isFirst = false,
                isLast = true,
            )
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(0.5.dp, DividerColor, RoundedCornerShape(12.dp))
            .background(SurfaceColor),
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // Icon circle
        Text(
            text = icon,
            fontSize = 18.sp,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceElevated)
                .padding(8.dp),
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp,
            )
        }

        Text(text = "›", color = TextMuted, fontSize = 20.sp)
    }
}
