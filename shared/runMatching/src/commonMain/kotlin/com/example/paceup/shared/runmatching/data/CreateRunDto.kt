package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.runmatching.domain.CreateRunParams
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serialized INSERT body for the `runs` Supabase table.
 * Excludes auto-generated fields: id, status (defaults "open"), created_at, updated_at.
 */
@Serializable
internal data class CreateRunDto(
    @SerialName("creator_id") val creatorId: String,
    @SerialName("title") val title: String?,
    @SerialName("description") val description: String?,
    @SerialName("mode") val mode: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("meeting_lat") val meetingLat: Double,
    @SerialName("meeting_lng") val meetingLng: Double,
    @SerialName("meeting_address") val meetingAddress: String,
    @SerialName("city") val city: String,
    @SerialName("distance_km") val distanceKm: Float?,
    @SerialName("duration_min") val durationMin: Int?,
    @SerialName("pace_min_sec") val paceMinSec: Int,
    @SerialName("pace_max_sec") val paceMaxSec: Int,
    @SerialName("max_participants") val maxParticipants: Int?,
    @SerialName("age_min") val ageMin: Int?,
    @SerialName("age_max") val ageMax: Int?,
    @SerialName("gender_filter") val genderFilter: String,
    @SerialName("verified_only") val verifiedOnly: Boolean,
    @SerialName("join_mode") val joinMode: String,
    @SerialName("is_recurring") val isRecurring: Boolean,
    @SerialName("recurrence_rule") val recurrenceRule: String?,
)

internal fun CreateRunParams.toDto() = CreateRunDto(
    creatorId = creatorId,
    title = title,
    description = description,
    mode = mode.value,
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
)
