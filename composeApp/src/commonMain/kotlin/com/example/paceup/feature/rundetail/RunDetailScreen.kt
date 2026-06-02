package com.example.paceup.feature.rundetail

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paceup.feature.home.ZoneBadge
import com.example.paceup.feature.home.color
import com.example.paceup.feature.home.formatPaceRange
import com.example.paceup.feature.home.paceZone
import com.example.paceup.shared.pacezone.PaceZone
import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.RunParticipant
import com.example.paceup.shared.runmatching.domain.RunStatus
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

private val BackgroundColor = Color(0xFF0D1B2A)
private val SurfaceColor = Color(0xFF1F2937)
private val SurfaceElevated = Color(0xFF374151)
private val DividerColor = Color(0xFF374151)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)
private val SuccessGreen = Color(0xFF10B981)

// ── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun RunDetailRoot(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (runId: String, runTitle: String) -> Unit = { _, _ -> },
    onNavigateToUserProfile: (userId: String) -> Unit = {},
    onNavigateToRatePartners: (runId: String, runTitle: String) -> Unit = { _, _ -> },
    viewModel: RunDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RunDetailEvent.NavigateBack -> onNavigateBack()
            is RunDetailEvent.NavigateToChat -> onNavigateToChat(event.runId, event.runTitle)
            is RunDetailEvent.NavigateToUserProfile -> onNavigateToUserProfile(event.userId)
            is RunDetailEvent.NavigateToRatePartners -> onNavigateToRatePartners(event.runId, event.runTitle)
        }
    }

    RunDetailScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun RunDetailScreen(
    state: RunDetailState,
    onAction: (RunDetailAction) -> Unit,
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
            state.error != null && state.run == null -> {
                ErrorContent(
                    message = state.error.asString(),
                    onRetry = { onAction(RunDetailAction.OnRetry) },
                    onBack = { onAction(RunDetailAction.OnBackClick) },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            state.run != null -> {
                RunDetailContent(
                    state = state,
                    onAction = onAction,
                )
            }
            else -> {
                // empty — loading has not started yet
            }
        }

        // Back button always visible at top-left
        BackButton(
            onClick = { onAction(RunDetailAction.OnBackClick) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .systemBarsPadding()
                .padding(12.dp),
        )
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
private fun RunDetailContent(
    state: RunDetailState,
    onAction: (RunDetailAction) -> Unit,
) {
    val run = state.run ?: return
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable body — padded at bottom so sticky join button doesn't cover content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(top = 56.dp, bottom = 96.dp),
        ) {
            // ── Hero ─────────────────────────────────────────────────────────
            HeroSection(run = run)

            Spacer(Modifier.height(16.dp))

            // ── Info cards ───────────────────────────────────────────────────
            InfoSection(run = run)

            Spacer(Modifier.height(16.dp))

            // ── Requirements ─────────────────────────────────────────────────
            RequirementsSection(run = run)

            // ── Pending requests (creator only) ───────────────────────────────
            if (state.isCreator && state.pendingRequests.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                PendingRequestsSection(
                    requests = state.pendingRequests,
                    onAccept = { onAction(RunDetailAction.OnAcceptParticipant(it)) },
                    onDecline = { onAction(RunDetailAction.OnDeclineParticipant(it)) },
                )
            }

            // ── Accepted participants ─────────────────────────────────────────
            if (state.participants.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                ParticipantsSection(
                    participants = state.participants,
                    maxParticipants = run.maxParticipants,
                    onParticipantClick = { onAction(RunDetailAction.OnParticipantClick(it)) },
                )
            }

            // ── Group chat button (accepted participant or creator) ────────────
            val showChat = state.isCreator || state.joinStatus == JoinStatus.JOINED
            if (showChat) {
                Spacer(Modifier.height(16.dp))
                ChatButton(
                    onClick = { onAction(RunDetailAction.OnChatClick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            // ── Rate partners button (attended participants only) ───────────────
            if (state.userAttended) {
                Spacer(Modifier.height(8.dp))
                RatePartnersButton(
                    onClick = { onAction(RunDetailAction.OnRatePartnersClick) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            // ── Description ──────────────────────────────────────────────────
            val description = run.description
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                DescriptionSection(description = description)
            }
        }

        // ── Sticky bottom action ──────────────────────────────────────────────
        if (state.isCreator) {
            CreatorCancelButton(
                state = state,
                onAction = onAction,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        } else {
            JoinButton(
                state = state,
                onAction = onAction,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    // Cancel run confirmation dialog
    if (state.showCancelRunDialog) {
        CancelRunDialog(
            onConfirm = { reason -> onAction(RunDetailAction.OnConfirmCancelRun(reason)) },
            onDismiss = { onAction(RunDetailAction.OnDismissCancelRunDialog) },
        )
    }
}

// ── Hero section ──────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(run: Run) {
    val zone = run.paceZone()
    val zoneColor = zone.color()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .border(width = 0.dp, color = Color.Transparent) // no top border — blends with bg
    ) {
        // Left zone accent bar
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(120.dp)
                .align(Alignment.TopStart)
                .background(zoneColor),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
        ) {
            // Status chip + mode chip row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(status = run.status)
                ModeChip(mode = run.mode.name)
                if (run.isRecurring) RecurringBadge()
            }

            Spacer(Modifier.height(12.dp))

            // Title
            Text(
                text = run.title ?: run.mode.name.replaceFirstChar { it.uppercase() } + " Run",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
            )

            Spacer(Modifier.height(10.dp))

            // Zone badge + pace range
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ZoneBadge(
                    zone = zone.name,
                    paceRange = formatPaceRange(run.paceMinSec, run.paceMaxSec),
                    color = zoneColor,
                )
                Text(
                    text = formatPaceRange(run.paceMinSec, run.paceMaxSec),
                    color = zoneColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Info section ──────────────────────────────────────────────────────────────

@Composable
private fun InfoSection(run: Run) {
    SectionCard {
        InfoRow(
            icon = "📅",
            label = "Date & time",
            value = formatDateTime(run.scheduledAt),
        )
        SectionDivider()
        InfoRow(
            icon = "📍",
            label = "Meeting point",
            value = "${run.meetingAddress}\n${run.city}",
        )
        SectionDivider()
        val distanceStr = run.distanceKm?.let { "${it.toInt()} km" }
            ?: run.durationMin?.let { "${it} min" }
            ?: "—"
        InfoRow(
            icon = "🏃",
            label = "Distance",
            value = distanceStr,
        )
        if (run.maxParticipants != null) {
            SectionDivider()
            InfoRow(
                icon = "👥",
                label = "Max participants",
                value = "${run.maxParticipants} runners",
            )
        }
    }
}

// ── Requirements section ──────────────────────────────────────────────────────

@Composable
private fun RequirementsSection(run: Run) {
    SectionCard {
        InfoRow(
            icon = "🔗",
            label = "Join mode",
            value = when (run.joinMode) {
                "open" -> "Open — anyone can join"
                "request" -> "Request — creator approves"
                "invite_only" -> "Invite only"
                else -> run.joinMode
            },
        )
        if (run.verifiedOnly) {
            SectionDivider()
            InfoRow(
                icon = "✓",
                label = "Verification",
                value = "Verified runners only",
                valueColor = SuccessGreen,
            )
        }
    }
}

// ── Participants section ──────────────────────────────────────────────────────

@Composable
private fun ParticipantsSection(
    participants: List<RunParticipant>,
    maxParticipants: Int?,
    onParticipantClick: (String) -> Unit = {},
) {
    val countLabel = if (maxParticipants != null)
        "${participants.size} / $maxParticipants joined"
    else
        "${participants.size} joined"

    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = "👥  $countLabel",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

        // Avatar stack
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            val visible = participants.take(6)
            val overflow = participants.size - visible.size
            Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                visible.forEachIndexed { index, participant ->
                    ParticipantAvatar(
                        participant = participant,
                        modifier = Modifier.offset(x = (index * 0).dp), // spacing handled by arrangement
                    )
                }
                if (overflow > 0) {
                    OverflowAvatar(count = overflow)
                }
            }
        }

        // Participant name list — tappable to view profile
        participants.forEach { participant ->
            ParticipantRow(
                participant = participant,
                onClick = { onParticipantClick(participant.userId) },
            )
        }
    }
}

@Composable
private fun ParticipantAvatar(
    participant: RunParticipant,
    modifier: Modifier = Modifier,
) {
    val zoneColor = participant.paceZone?.let { zoneName ->
        runCatching { PaceZone.valueOf(zoneName).color() }.getOrNull()
    } ?: Color(0xFF6B7280)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(zoneColor.copy(alpha = 0.2f))
            .border(2.5.dp, zoneColor, CircleShape),
    ) {
        Text(
            text = participant.displayName.take(1).uppercase(),
            color = zoneColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OverflowAvatar(count: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SurfaceElevated)
            .border(2.5.dp, DividerColor, CircleShape),
    ) {
        Text(
            text = "+$count",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ParticipantRow(participant: RunParticipant, onClick: () -> Unit = {}) {
    val zoneColor = participant.paceZone?.let { zoneName ->
        runCatching { PaceZone.valueOf(zoneName).color() }.getOrNull()
    } ?: Color(0xFF6B7280)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        // Mini avatar with zone ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(zoneColor.copy(alpha = 0.15f))
                .border(2.dp, zoneColor, CircleShape),
        ) {
            Text(
                text = participant.displayName.take(1).uppercase(),
                color = zoneColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = participant.displayName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (participant.reputationTier == "trusted" || participant.reputationTier == "pacer_eligible") {
                    TrustedBadge()
                }
            }
            participant.paceZone?.let {
                Text(
                    text = "Zone $it",
                    color = zoneColor,
                    fontSize = 12.sp,
                )
            }
        }

        // Show-up rate
        participant.showUpRate?.let { rate ->
            val rateColor = when {
                rate >= 0.85f -> SuccessGreen
                rate >= 0.70f -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }
            Text(
                text = "${(rate * 100).toInt()}%",
                color = rateColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

// ── Description section ───────────────────────────────────────────────────────

@Composable
private fun DescriptionSection(description: String) {
    SectionCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "About this run",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

// ── Pending requests section (creator only) ───────────────────────────────────

@Composable
private fun PendingRequestsSection(
    requests: List<RunParticipant>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
) {
    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = "⏳  ${requests.size} pending request${if (requests.size != 1) "s" else ""}",
                color = Color(0xFFF59E0B),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        requests.forEach { request ->
            PendingRequestRow(
                participant = request,
                onAccept = { onAccept(request.userId) },
                onDecline = { onDecline(request.userId) },
            )
        }
    }
}

@Composable
private fun PendingRequestRow(
    participant: RunParticipant,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val zoneColor = participant.paceZone?.let { zoneName ->
        runCatching { PaceZone.valueOf(zoneName).color() }.getOrNull()
    } ?: Color(0xFF6B7280)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar with zone ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(zoneColor.copy(alpha = 0.15f))
                    .border(2.dp, zoneColor, CircleShape),
            ) {
                Text(
                    text = participant.displayName.take(1).uppercase(),
                    color = zoneColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.displayName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    participant.paceZone?.let {
                        Text(text = "Zone $it", color = zoneColor, fontSize = 12.sp)
                    }
                    participant.showUpRate?.let { rate ->
                        val rateColor = when {
                            rate >= 0.85f -> SuccessGreen
                            rate >= 0.70f -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        }
                        Text(
                            text = "${(rate * 100).toInt()}% show-up",
                            color = rateColor,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Decline button (outlined)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                    .clickable { onDecline() }
                    .padding(vertical = 8.dp),
            ) {
                Text(text = "Decline", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            // Accept button (filled green)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SuccessGreen)
                    .clickable { onAccept() }
                    .padding(vertical = 8.dp),
            ) {
                Text(text = "Accept", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

// ── Join button ───────────────────────────────────────────────────────────────

@Composable
private fun JoinButton(
    state: RunDetailState,
    onAction: (RunDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val run = state.run ?: return
    if (state.isCreator) return  // Creator manages participants — no join button
    if (state.userAttended) return  // Attended — run is done, rate partners instead

    val (label, enabled, onClick) = when {
        state.isJoining -> Triple("…", false, null)
        run.status == RunStatus.CANCELLED -> Triple("Cancelled", false, null)
        run.status == RunStatus.COMPLETED -> Triple("Run completed", false, null)
        run.joinMode == "invite_only" -> Triple("Invite only", false, null)
        run.status == RunStatus.FULL && state.joinStatus == JoinStatus.NONE ->
            Triple("Run full", false, null)
        state.joinStatus == JoinStatus.JOINED ->
            Triple("Leave run", true, RunDetailAction.OnCancelParticipationClick)
        state.joinStatus == JoinStatus.REQUESTED ->
            Triple("Cancel request", true, RunDetailAction.OnCancelParticipationClick)
        !state.canJoin ->
            Triple("Verified runners only", false, null)
        run.joinMode == "request" ->
            Triple("Request to join →", true, RunDetailAction.OnJoinClick)
        else -> Triple("Join run →", true, RunDetailAction.OnJoinClick)
    }

    val bg = when {
        state.isJoining -> SurfaceElevated
        state.joinStatus == JoinStatus.JOINED -> Color(0xFF0F6E56)  // success green — leave
        state.joinStatus == JoinStatus.REQUESTED -> SurfaceElevated
        enabled -> PrimaryBlue
        else -> SurfaceElevated
    }
    val textColor = if (enabled || state.isJoining) Color.White else TextMuted

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .then(if (enabled && onClick != null) Modifier.clickable { onAction(onClick) } else Modifier)
            .padding(vertical = 16.dp),
    ) {
        if (state.isJoining) {
            CircularProgressIndicator(
                color = TextMuted,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = label,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ── Trusted badge (inline) ────────────────────────────────────────────────────

@Composable
private fun TrustedBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(SuccessGreen.copy(alpha = 0.12f))
            .border(0.5.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "✓",
            color = SuccessGreen,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Rate partners button ──────────────────────────────────────────────────────

@Composable
private fun RatePartnersButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SuccessGreen.copy(alpha = 0.08f))
            .border(1.dp, SuccessGreen.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text = "⭐", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Rate partners",
            color = SuccessGreen,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Group chat button ─────────────────────────────────────────────────────────

@Composable
private fun ChatButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text = "💬", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Group Chat",
            color = PrimaryBlue,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Creator cancel button ──────────────────────────────────────────────────────

@Composable
private fun CreatorCancelButton(
    state: RunDetailState,
    onAction: (RunDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val run = state.run ?: return
    val alreadyCancelled = run.status == com.example.paceup.shared.runmatching.domain.RunStatus.CANCELLED

    val (label, enabled) = when {
        state.isCancellingRun -> "Cancelling…" to false
        alreadyCancelled -> "Run cancelled" to false
        else -> "Cancel run" to true
    }
    val bg = if (alreadyCancelled) SurfaceElevated else Color(0xFFA32D2D)
    val textColor = if (enabled) Color.White else TextMuted

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .then(if (enabled) Modifier.clickable { onAction(RunDetailAction.OnCancelRunClick) } else Modifier)
            .padding(vertical = 16.dp),
    ) {
        if (state.isCancellingRun) {
            CircularProgressIndicator(color = TextMuted, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(text = label, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CancelRunDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = {
            Text(text = "Cancel this run?", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "All accepted participants will be notified. This cannot be undone.",
                    color = TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text("Reason (optional)", color = TextMuted, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DividerColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim()) }) {
                Text("Yes, cancel run", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep run", color = TextMuted)
            }
        },
    )
}

// ── Back button ───────────────────────────────────────────────────────────────

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

// ── Error content ─────────────────────────────────────────────────────────────

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
private fun SectionDivider() {
    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
}

@Composable
private fun InfoRow(
    icon: String,
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text = icon, fontSize = 16.sp, modifier = Modifier.padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun StatusChip(status: RunStatus) {
    val (label, color) = when (status) {
        RunStatus.OPEN -> "Open" to SuccessGreen
        RunStatus.FULL -> "Full" to Color(0xFFF59E0B)
        RunStatus.IN_PROGRESS -> "In progress" to PrimaryBlue
        RunStatus.COMPLETED -> "Completed" to TextMuted
        RunStatus.CANCELLED -> "Cancelled" to Color(0xFFEF4444)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun ModeChip(mode: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = mode.replace("_", " ").uppercase(),
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun RecurringBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(PrimaryBlue.copy(alpha = 0.15f))
            .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "🔁 RECURRING",
            color = PrimaryBlue,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
}

// ── Date formatting ───────────────────────────────────────────────────────────

private fun formatDateTime(scheduledAt: String): String {
    return try {
        // Normalize ISO string — add Z if no offset present
        val normalized = when {
            scheduledAt.endsWith("Z") -> scheduledAt
            scheduledAt.contains("+") -> scheduledAt
            else -> "${scheduledAt}Z"
        }
        val instant = Instant.parse(normalized)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = local.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        val month = local.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val minute = local.minute.toString().padStart(2, '0')
        "$day, $month ${local.dayOfMonth}  •  ${local.hour}:$minute"
    } catch (_: Exception) {
        scheduledAt
    }
}
