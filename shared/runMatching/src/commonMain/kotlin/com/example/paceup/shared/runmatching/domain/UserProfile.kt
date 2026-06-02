package com.example.paceup.shared.runmatching.domain

/**
 * Full user profile as shown on the profile screen (spec §4.1).
 * All data is derived from Strava/Garmin — no self-reported pace or distance.
 */
data class UserProfile(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val city: String?,
    /** Verified pace zone letter: A / B / C / D / E. */
    val paceZone: String?,
    /** Rolling average pace in seconds/km from last 90 days (spec §4.1). */
    val avgPaceSeconds: Int?,
    /** Rolling 8-week average weekly km (spec §4.1). */
    val weeklyMileageAvg: Float?,
    /** Longest run in last 90 days in km (spec §4.1). */
    val longestRunKm: Float?,
    /** Show-up rate 0.0–1.0. Color-coded on profile (spec §4.4). */
    val showUpRate: Float?,
    /** Total group runs completed on PaceUp (spec §4.1). */
    val totalPaceupRuns: Int?,
    /** Distinct co-runners (spec §4.1). */
    val uniquePartners: Int?,
    /** Reputation tier: new_runner / active / trusted / pacer_eligible (spec §4.4). */
    val reputationTier: String?,
    val stravaConnected: Boolean,
    val garminConnected: Boolean,
)
