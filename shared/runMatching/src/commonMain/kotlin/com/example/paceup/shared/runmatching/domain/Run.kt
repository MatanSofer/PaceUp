package com.example.paceup.shared.runmatching.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Run modes as defined in SPEC.md §8.2 (runs.mode). */
enum class RunMode(val value: String) {
    EASY("easy"),
    TEMPO("tempo"),
    RACE_PREP("race_prep"),
    RECOVERY("recovery"),
    TOURIST("tourist"),
    PACER("pacer");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: EASY
    }
}

/** Lifecycle status of a run (runs.status). */
enum class RunStatus(val value: String) {
    OPEN("open"),
    FULL("full"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: OPEN
    }
}

/**
 * Domain model for a single group run.
 * Maps to the `runs` table in Supabase (SPEC.md §8.2).
 */
data class Run(
    val id: String,
    val creatorId: String,
    val title: String?,
    val description: String?,
    val mode: RunMode,
    val status: RunStatus,
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
    /** Gender filter: "any" | "male" | "female". */
    val genderFilter: String = "any",
    val verifiedOnly: Boolean,
    val joinMode: String,
    val isRecurring: Boolean,
    /** "weekly" | "biweekly" — null for one-time runs. */
    val recurrenceRule: String? = null,
    val cancellationReason: String? = null,
    val createdAt: String,
)

/** Filters for run discovery queries. */
data class RunFilters(
    val paceMinSec: Int? = null,
    val paceMaxSec: Int? = null,
    val modes: List<RunMode> = emptyList(),
    val verifiedOnly: Boolean? = null,
    val maxDistanceKm: Float? = null,
    val minDistanceKm: Float? = null,
    val afterDate: String? = null,
    val beforeDate: String? = null,
    val openJoinOnly: Boolean? = null,
    val recurringOnly: Boolean? = null,
)

/** Raw Supabase row shape for the `runs` table. */
@Serializable
internal data class RunDto(
    @SerialName("id") val id: String,
    @SerialName("creator_id") val creatorId: String,
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("mode") val mode: String,
    @SerialName("status") val status: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("meeting_lat") val meetingLat: Double,
    @SerialName("meeting_lng") val meetingLng: Double,
    @SerialName("meeting_address") val meetingAddress: String,
    @SerialName("city") val city: String,
    @SerialName("distance_km") val distanceKm: Float? = null,
    @SerialName("duration_min") val durationMin: Int? = null,
    @SerialName("pace_min_sec") val paceMinSec: Int,
    @SerialName("pace_max_sec") val paceMaxSec: Int,
    @SerialName("max_participants") val maxParticipants: Int? = null,
    @SerialName("age_min") val ageMin: Int? = null,
    @SerialName("age_max") val ageMax: Int? = null,
    @SerialName("gender_filter") val genderFilter: String = "any",
    @SerialName("verified_only") val verifiedOnly: Boolean,
    @SerialName("join_mode") val joinMode: String,
    @SerialName("is_recurring") val isRecurring: Boolean,
    @SerialName("recurrence_rule") val recurrenceRule: String? = null,
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    @SerialName("created_at") val createdAt: String,
)

internal fun RunDto.toDomain() = Run(
    id = id,
    creatorId = creatorId,
    title = title,
    description = description,
    mode = RunMode.from(mode),
    status = RunStatus.from(status),
    scheduledAt = scheduledAt,
    meetingLat = meetingLat,
    meetingLng = meetingLng,
    meetingAddress = meetingAddress,
    city = city,
    distanceKm = distanceKm,
    durationMin = durationMin,
    paceMinSec = paceMinSec,
    paceMaxSec = paceMaxSec,
    maxParticipants = maxParticipants,
    ageMin = ageMin,
    ageMax = ageMax,
    genderFilter = genderFilter,
    verifiedOnly = verifiedOnly,
    joinMode = joinMode,
    isRecurring = isRecurring,
    recurrenceRule = recurrenceRule,
    cancellationReason = cancellationReason,
    createdAt = createdAt,
)
