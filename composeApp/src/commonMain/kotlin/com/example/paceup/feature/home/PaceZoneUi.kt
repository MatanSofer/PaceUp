package com.example.paceup.feature.home

import androidx.compose.ui.graphics.Color
import com.example.paceup.shared.pacezone.PaceZone
import com.example.paceup.shared.runmatching.domain.Run

/** Zone color per the UI design skill — pace zones are the visual language of speed. */
fun PaceZone.color(): Color = when (this) {
    PaceZone.A -> Color(0xFFA855F7) // Purple — elite
    PaceZone.B -> Color(0xFF3B82F6) // Blue — advanced
    PaceZone.C -> Color(0xFF10B981) // Green — intermediate
    PaceZone.D -> Color(0xFFF59E0B) // Amber — recreational
    PaceZone.E -> Color(0xFF6B7280) // Gray — casual
}

/** Derives the display pace zone for a run from its pace range midpoint. */
fun Run.paceZone(): PaceZone = PaceZone.fromPace((paceMinSec + paceMaxSec) / 2)

/** Formats a pace range as "5:00–5:30 /km". */
fun formatPaceRange(paceMinSec: Int, paceMaxSec: Int): String {
    fun fmt(s: Int) = "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    return "${fmt(paceMinSec)}–${fmt(paceMaxSec)} /km"
}
