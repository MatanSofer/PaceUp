package com.example.paceup.feature.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.feature.home.formatPaceRange
import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.UserProfile
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import org.koin.compose.viewmodel.koinViewModel

private val BackgroundColor = Color(0xFF0D1B2A)
private val SurfaceColor = Color(0xFF1F2937)
private val SurfaceElevated = Color(0xFF374151)
private val DividerColor = Color(0xFF374151)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)
private val SuccessGreen = Color(0xFF10B981)
private val AccentOrange = Color(0xFFFC4C02)

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun UserProfileRoot(
    onNavigateBack: () -> Unit,
    viewModel: UserProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            UserProfileEvent.NavigateBack -> onNavigateBack()
        }
    }

    UserProfileScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun UserProfileScreen(
    state: UserProfileState,
    onAction: (UserProfileAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    color = PrimaryBlue,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            state.error != null -> {
                ErrorContent(
                    message = state.error.asString(),
                    onRetry = { onAction(UserProfileAction.OnRetry) },
                    onBack = { onAction(UserProfileAction.OnBackClick) },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            state.profile != null -> {
                ProfileContent(
                    state = state,
                )
            }
        }

        BackButton(
            onClick = { onAction(UserProfileAction.OnBackClick) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .systemBarsPadding()
                .padding(12.dp),
        )
    }
}

// ── Profile content ───────────────────────────────────────────────────────────

@Composable
private fun ProfileContent(state: UserProfileState) {
    val profile = state.profile ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(top = 56.dp, bottom = 24.dp),
    ) {
        // ── Hero ─────────────────────────────────────────────────────────────
        HeroSection(profile = profile)

        Spacer(Modifier.height(16.dp))

        // ── Stats row ────────────────────────────────────────────────────────
        StatsRow(profile = profile)

        Spacer(Modifier.height(16.dp))

        // ── Activity metrics ─────────────────────────────────────────────────
        ActivitySection(profile = profile)

        Spacer(Modifier.height(16.dp))

        // ── Connected apps ───────────────────────────────────────────────────
        ConnectedAppsSection(profile = profile)

        // ── Recent runs ──────────────────────────────────────────────────────
        if (state.recentRuns.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            RecentRunsSection(runs = state.recentRuns)
        }
    }
}

// ── Hero section ──────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(profile: UserProfile) {
    val zoneColor = when (profile.paceZone) {
        "A" -> Color(0xFF7C3AED)
        "B" -> Color(0xFF1D6FA8)
        "C" -> Color(0xFF0F6E56)
        "D" -> Color(0xFFBA7517)
        else -> Color(0xFF6B7280)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor),
    ) {
        // Zone color accent stripe at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(zoneColor),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            // Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(zoneColor.copy(alpha = 0.2f))
                    .border(3.dp, zoneColor, CircleShape),
            ) {
                Text(
                    text = profile.displayName.take(1).uppercase(),
                    color = zoneColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = profile.displayName,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            profile.city?.let {
                Spacer(Modifier.height(4.dp))
                Text(text = "📍 $it", color = TextMuted, fontSize = 13.sp)
            }

            Spacer(Modifier.height(12.dp))

            // Pace zone badge + show-up rate
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                profile.paceZone?.let { zone ->
                    ZoneBadge(zone = zone, color = zoneColor, paceSeconds = profile.avgPaceSeconds)
                }
                profile.showUpRate?.let { rate ->
                    ShowUpBadge(rate = rate)
                }
                ReputationBadge(tier = profile.reputationTier)
            }

            // Bio
            profile.bio?.let { bio ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = bio,
                    color = TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun ZoneBadge(zone: String, color: Color, paceSeconds: Int?) {
    val paceLabel = if (paceSeconds != null) {
        val min = paceSeconds / 60
        val sec = paceSeconds % 60
        "Zone $zone  •  ${min}:${sec.toString().padStart(2, '0')}/km"
    } else {
        "Zone $zone"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text = paceLabel, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ShowUpBadge(rate: Float) {
    val percent = (rate * 100).toInt()
    val color = when {
        rate >= 0.85f -> SuccessGreen
        rate >= 0.70f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text = "$percent% show-up", color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReputationBadge(tier: String?) {
    val (label, color) = when (tier) {
        "trusted" -> "✓ Trusted" to SuccessGreen
        "pacer_eligible" -> "⚡ Pacer" to PrimaryBlue
        "active" -> "Active" to TextMuted
        else -> return
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Stats row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(profile: UserProfile) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        StatCard(
            label = "Runs",
            value = profile.totalPaceupRuns?.toString() ?: "—",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Partners",
            value = profile.uniquePartners?.toString() ?: "—",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Avg/wk",
            value = profile.weeklyMileageAvg?.let { "${it.toInt()} km" } ?: "—",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = TextMuted, fontSize = 11.sp)
    }
}

// ── Activity section ──────────────────────────────────────────────────────────

@Composable
private fun ActivitySection(profile: UserProfile) {
    SectionCard {
        SectionHeader(label = "Running Stats")
        val longestRun = profile.longestRunKm
        if (longestRun != null) {
            InfoRow(label = "Longest recent run", value = "${longestRun.toInt()} km")
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        }
        val mileage = profile.weeklyMileageAvg
        if (mileage != null) {
            InfoRow(label = "Weekly avg distance", value = "${mileage.toInt()} km")
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        }
        val pace = profile.avgPaceSeconds
        if (pace != null) {
            val min = pace / 60
            val sec = pace % 60
            InfoRow(
                label = "Verified avg pace",
                value = "${min}:${sec.toString().padStart(2, '0')} /km",
            )
        }
    }
}

// ── Connected apps section ────────────────────────────────────────────────────

@Composable
private fun ConnectedAppsSection(profile: UserProfile) {
    SectionCard {
        SectionHeader(label = "Connected Apps")
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            AppBadge(
                name = "Strava",
                connected = profile.stravaConnected,
                color = AccentOrange,
            )
            AppBadge(
                name = "Garmin",
                connected = profile.garminConnected,
                color = Color(0xFF006CB7),
            )
        }
    }
}

@Composable
private fun AppBadge(name: String, connected: Boolean, color: Color) {
    val activeColor = if (connected) color else TextMuted
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(activeColor.copy(alpha = 0.1f))
            .border(1.dp, activeColor.copy(alpha = if (connected) 0.6f else 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = if (connected) "✓ $name" else "$name",
            color = activeColor,
            fontSize = 12.sp,
            fontWeight = if (connected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ── Recent runs section ───────────────────────────────────────────────────────

@Composable
private fun RecentRunsSection(runs: List<Run>) {
    SectionCard {
        SectionHeader(label = "Recent Runs")
        runs.forEach { run ->
            RecentRunRow(run = run)
            if (run != runs.last()) {
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun RecentRunRow(run: Run) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = run.title ?: "${run.mode.name.replaceFirstChar { it.uppercase() }} Run",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${run.city}  •  ${run.distanceKm?.let { "${it.toInt()} km" } ?: "—"}",
                color = TextMuted,
                fontSize = 12.sp,
            )
        }
        Text(
            text = formatPaceRange(run.paceMinSec, run.paceMaxSec),
            color = PrimaryBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Shared sub-components ─────────────────────────────────────────────────────

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp)),
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SurfaceColor.copy(alpha = 0.9f))
            .border(1.dp, DividerColor, CircleShape)
            .clickable { onClick() },
    ) {
        Text(text = "←", color = TextPrimary, fontSize = 18.sp)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(32.dp),
    ) {
        Text(text = "⚠️", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(text = message, color = TextMuted, fontSize = 15.sp)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(100.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(text = "Back", color = TextMuted, fontSize = 14.sp)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(PrimaryBlue)
                    .clickable { onRetry() }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(text = "Retry", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
