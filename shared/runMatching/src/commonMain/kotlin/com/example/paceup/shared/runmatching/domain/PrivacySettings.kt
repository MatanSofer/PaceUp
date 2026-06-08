package com.example.paceup.shared.runmatching.domain

/** Privacy settings for the current user (spec §5.2 Privacy). */
data class PrivacySettings(
    val profileVisibility: String = "public",   // "public" | "friends" | "private"
    val showPaceZone: Boolean = true,
    val showRunHistory: Boolean = true,
    val showRivals: Boolean = true,
    val locationPrecision: String = "city",      // "city" | "country"
)
