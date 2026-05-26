package com.example.paceup.shared.runmatching.domain

/**
 * All fields needed to insert a new run into Supabase.
 * Used by [RunRepository.createRun] (SPEC.md §4.2 — Run Parameters).
 *
 * @param creatorId The authenticated user's ID — obtained from the auth session by the caller.
 * @param scheduledAt ISO-8601 timestamp, e.g. "2026-06-01T06:30:00Z".
 * @param genderFilter "any" | "male" | "female". Defaults to "any".
 * @param joinMode "open" | "request" | "invite_only". Defaults to "open".
 * @param recurrenceRule "weekly" | "biweekly" — null for one-time runs.
 */
data class CreateRunParams(
    val creatorId: String,
    val title: String?,
    val description: String?,
    val mode: RunMode,
    val scheduledAt: String,
    val meetingLat: Double,
    val meetingLng: Double,
    val meetingAddress: String,
    val city: String,
    val distanceKm: Float?,
    val durationMin: Int?,
    val paceMinSec: Int,
    val paceMaxSec: Int,
    val maxParticipants: Int?,
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val genderFilter: String = "any",
    val verifiedOnly: Boolean = false,
    val joinMode: String = "open",
    val isRecurring: Boolean = false,
    val recurrenceRule: String? = null,
)
