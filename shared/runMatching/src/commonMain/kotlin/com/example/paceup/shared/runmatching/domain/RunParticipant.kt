package com.example.paceup.shared.runmatching.domain

/**
 * A participant of a run, with enough profile data to render the detail screen.
 * Maps to a joined query of `run_participants` and `users` tables (SPEC.md §4.4).
 */
data class RunParticipant(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    /** Raw zone letter stored in users.pace_zone — "A" | "B" | "C" | "D" | "E" | null. */
    val paceZone: String?,
    /** Show-up rate as a value 0.0–1.0 from users.show_up_rate. */
    val showUpRate: Float?,
    /** Participation status — "accepted", "pending", "declined", etc. */
    val status: String,
)
