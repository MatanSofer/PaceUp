package com.example.paceup.shared.runmatching.domain

/** Public profile snapshot used in search results, rival requests, and person lookups. */
data class UserSummary(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    /** Raw zone letter — "A" | "B" | "C" | "D" | "E" | null. */
    val paceZone: String?,
    /** Show-up rate 0.0–1.0, null if no history. */
    val showUpRate: Float?,
)
