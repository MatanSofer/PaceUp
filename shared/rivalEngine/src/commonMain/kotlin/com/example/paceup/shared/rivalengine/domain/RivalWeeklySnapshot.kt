package com.example.paceup.shared.rivalengine.domain

/**
 * Aggregated stats for one rivalry for a single ISO week.
 * Written by the `rival_weekly_snapshot` Edge Function every Monday (spec §8.4).
 *
 * @param rivalId FK to the parent [Rival].
 * @param weekStart ISO date of the Monday that started this week (e.g. "2026-06-02").
 * @param userAKm Total km run by user A this week.
 * @param userBKm Total km run by user B this week.
 * @param userARuns Number of runs completed by user A.
 * @param userBRuns Number of runs completed by user B.
 * @param userABestPace Fastest pace (sec/km) recorded by user A, null if no runs.
 * @param userBBestPace Fastest pace (sec/km) recorded by user B, null if no runs.
 * @param winner Result once week is closed: "user_a" | "user_b" | "tied" | null (week in progress).
 */
data class RivalWeeklySnapshot(
    val rivalId: String,
    val weekStart: String,
    val userAKm: Float,
    val userBKm: Float,
    val userARuns: Int,
    val userBRuns: Int,
    val userABestPace: Int?,
    val userBBestPace: Int?,
    val winner: String?,
)
