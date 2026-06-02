package com.example.paceup.feature.partnerrating

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.example.paceup.feature.home.color
import com.example.paceup.shared.pacezone.PaceZone
import com.example.paceup.shared.runmatching.domain.PartnerTag
import com.example.paceup.ui.ObserveAsEvents
import com.example.paceup.ui.asString
import org.koin.compose.viewmodel.koinViewModel

private val BackgroundColor = Color(0xFF0D1B2A)
private val SurfaceColor = Color(0xFF1F2937)
private val DividerColor = Color(0xFF374151)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)
private val SuccessGreen = Color(0xFF10B981)

// ── Root ──────────────────────────────────────────────────────────────────────

@Composable
fun PartnerRatingRoot(
    onNavigateBack: () -> Unit,
    viewModel: PartnerRatingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            PartnerRatingEvent.NavigateBack -> onNavigateBack()
            PartnerRatingEvent.SubmitSuccess -> onNavigateBack()
        }
    }

    PartnerRatingScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun PartnerRatingScreen(
    state: PartnerRatingState,
    onAction: (PartnerRatingAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            TopBar(
                title = state.runTitle.ifBlank { "Rate Partners" },
                onBackClick = { onAction(PartnerRatingAction.OnBackClick) },
            )

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                state.alreadyRated -> {
                    AlreadyRatedContent(onBack = { onAction(PartnerRatingAction.OnBackClick) })
                }
                state.partners.isEmpty() -> {
                    EmptyPartnersContent(onBack = { onAction(PartnerRatingAction.OnBackClick) })
                }
                else -> {
                    PartnerListContent(
                        state = state,
                        onAction = onAction,
                    )
                }
            }
        }

        // Error message overlaid at bottom when partners are visible
        if (state.error != null && !state.isLoading) {
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                ErrorBanner(
                    message = state.error.asString(),
                    onDismiss = { onAction(PartnerRatingAction.OnDismissError) },
                )
            }
        }
    }
}

// ── Partner list + submit ─────────────────────────────────────────────────────

@Composable
private fun PartnerListContent(
    state: PartnerRatingState,
    onAction: (PartnerRatingAction) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "How were your running partners?",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            state.partners.forEachIndexed { index, partner ->
                PartnerCard(
                    partner = partner,
                    selectedTags = state.selectedTags[partner.userId] ?: emptySet(),
                    onTagToggle = { tag -> onAction(PartnerRatingAction.OnTagToggle(partner.userId, tag)) },
                )
                if (index < state.partners.lastIndex) {
                    HorizontalDivider(
                        color = DividerColor,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Sticky submit button
        SubmitButton(
            isSubmitting = state.isSubmitting,
            onClick = { onAction(PartnerRatingAction.OnSubmitClick) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

// ── Partner card ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartnerCard(
    partner: PartnerUi,
    selectedTags: Set<PartnerTag>,
    onTagToggle: (PartnerTag) -> Unit,
) {
    val zoneColor = partner.paceZone?.let { zoneName ->
        runCatching { PaceZone.valueOf(zoneName).color() }.getOrNull()
    } ?: Color(0xFF6B7280)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // ── Identity row ──────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar with pace zone ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(zoneColor.copy(alpha = 0.15f))
                    .border(2.dp, zoneColor, CircleShape),
            ) {
                Text(
                    text = partner.displayName.take(1).uppercase(),
                    color = zoneColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = partner.displayName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                partner.paceZone?.let { zone ->
                    Text(
                        text = "Zone $zone",
                        color = zoneColor,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Tag chips ─────────────────────────────────────────────────────────
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PartnerTag.entries.forEach { tag ->
                TagChip(
                    tag = tag,
                    selected = tag in selectedTags,
                    onClick = { onTagToggle(tag) },
                )
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: PartnerTag,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = tag.displayLabel()
    val chipColor = if (selected) SuccessGreen else Color(0xFF4B5563)
    val bgColor = if (selected) SuccessGreen.copy(alpha = 0.15f) else Color(0xFF1F2937)
    val textColor = if (selected) SuccessGreen else TextMuted

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .border(1.dp, chipColor.copy(alpha = if (selected) 0.7f else 0.4f), RoundedCornerShape(100.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun PartnerTag.displayLabel(): String = when (this) {
    PartnerTag.KEPT_PACE -> "Kept pace"
    PartnerTag.GREAT_ENERGY -> "Great energy"
    PartnerTag.PUSHED_GROUP -> "Pushed the group"
    PartnerTag.EARLY -> "Was early"
    PartnerTag.MISMATCHED_PACE -> "Mismatched pace"
}

// ── Submit button ─────────────────────────────────────────────────────────────

@Composable
private fun SubmitButton(
    isSubmitting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (isSubmitting) Color(0xFF374151) else PrimaryBlue)
            .then(if (!isSubmitting) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 16.dp),
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(color = TextMuted, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(
                text = "Submit ratings",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ── Empty / already-rated states ──────────────────────────────────────────────

@Composable
private fun AlreadyRatedContent(onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Text(text = "✅", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Already rated",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "You've already rated your partners for this run.",
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(28.dp))
        BackButtonCTA(onClick = onBack)
    }
}

@Composable
private fun EmptyPartnersContent(onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Text(text = "🏃", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No partners to rate",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No other verified attendees found for this run.",
            color = TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(28.dp))
        BackButtonCTA(onClick = onBack)
    }
}

@Composable
private fun BackButtonCTA(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(100.dp))
            .clickable { onClick() }
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Text(text = "Go back", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF7F1D1D))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(text = message, color = Color(0xFFF9FAFB), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "✕",
            color = TextMuted,
            fontSize = 14.sp,
            modifier = Modifier.clickable { onDismiss() },
        )
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(title: String, onBackClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF374151))
                .clickable { onBackClick() },
        ) {
            Text(text = "←", color = TextPrimary, fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
}
