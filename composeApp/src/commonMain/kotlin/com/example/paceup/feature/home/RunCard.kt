package com.example.paceup.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.RunMode

private val SurfaceColor = Color(0xFF1F2937)
private val DividerColor = Color(0xFF374151)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)
private val PrimaryBlue = Color(0xFF1A73E8)

/**
 * Run card matching the design spec layout.
 * Left-edge accent bar shows pace zone color.
 * @param scheduledTimeDisplay optional pre-formatted time string shown in the list view.
 */
@Composable
fun RunCard(
    run: Run,
    onJoinClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    scheduledTimeDisplay: String? = null,
) {
    val zone = run.paceZone()
    val zoneColor = zone.color()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            .clickable { onJoinClick(run.id) }
    ) {
        // Left zone accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(120.dp)
                .background(zoneColor)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Header row: zone badge + title + mode chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ZoneBadge(zone = zone.name, paceRange = formatPaceRange(run.paceMinSec, run.paceMaxSec), color = zoneColor)
                Text(
                    text = run.title ?: run.mode.displayName(),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                ModeChip(mode = run.mode)
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // Scheduled time (shown in list view)
            if (scheduledTimeDisplay != null) {
                Text(
                    text = "🕐 $scheduledTimeDisplay",
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
            }

            // Location
            Text(
                text = "📍 ${run.meetingAddress}",
                color = TextMuted,
                fontSize = 13.sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))

            // Pace + distance
            val distStr = run.distanceKm?.let { "  •  ${it.toInt()} km" } ?: ""
            Text(
                text = "🏃 ${formatPaceRange(run.paceMinSec, run.paceMaxSec)}$distStr",
                color = TextMuted,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // Footer: verified badge + join button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (run.verifiedOnly) {
                    Text(
                        text = "✓ Verified only",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(PrimaryBlue)
                        .clickable { onJoinClick(run.id) }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Join →",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** Pill badge showing zone letter + pace range. */
@Composable
fun ZoneBadge(zone: String, paceRange: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "ZONE $zone",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun ModeChip(mode: RunMode) {
    Box(
        modifier = Modifier
            .background(Color(0xFF374151), RoundedCornerShape(100.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = mode.displayName().uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted,
            letterSpacing = 0.5.sp,
        )
    }
}

private fun RunMode.displayName() = when (this) {
    RunMode.EASY -> "Easy"
    RunMode.TEMPO -> "Tempo"
    RunMode.RACE_PREP -> "Race Prep"
    RunMode.RECOVERY -> "Recovery"
    RunMode.TOURIST -> "Tourist"
    RunMode.PACER -> "Pacer"
}
