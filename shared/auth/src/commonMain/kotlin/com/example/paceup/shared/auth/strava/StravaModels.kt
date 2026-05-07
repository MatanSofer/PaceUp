package com.example.paceup.shared.auth.strava

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response from `POST /oauth/token`. */
@Serializable
data class StravaTokenDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_at") val expiresAt: Long,
    @SerialName("athlete") val athlete: StravaAthleteDto
)

@Serializable
data class StravaAthleteDto(
    @SerialName("id") val id: Long
)

/** Strava OAuth token after successful authorization. */
data class StravaToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val athleteId: Long
)

fun StravaTokenDto.toDomain() = StravaToken(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresAt = expiresAt,
    athleteId = athlete.id
)

/** Raw activity shape from `GET /api/v3/athlete/activities`. */
@Serializable
data class StravaActivityDto(
    @SerialName("id") val id: Long,
    @SerialName("type") val type: String,
    @SerialName("moving_time") val movingTimeSeconds: Int,
    @SerialName("distance") val distanceMeters: Float,
    @SerialName("average_speed") val averageSpeedMps: Float,
    @SerialName("start_date") val startDate: String
)

/**
 * Derived metrics extracted from a single Strava activity.
 * Raw activity data is never persisted — only these derived fields (spec §7.1).
 *
 * @param avgPaceSecondsPerKm Average pace in seconds per kilometre.
 * @param distanceKm Activity distance in kilometres.
 * @param date ISO-8601 start date.
 */
data class StravaActivityMetrics(
    val avgPaceSecondsPerKm: Int,
    val distanceKm: Float,
    val date: String
)

fun StravaActivityDto.toMetrics(): StravaActivityMetrics? {
    if (averageSpeedMps <= 0f) return null
    val paceSecPerKm = (1000f / averageSpeedMps).toInt()
    return StravaActivityMetrics(
        avgPaceSecondsPerKm = paceSecPerKm,
        distanceKm = distanceMeters / 1000f,
        date = startDate
    )
}
