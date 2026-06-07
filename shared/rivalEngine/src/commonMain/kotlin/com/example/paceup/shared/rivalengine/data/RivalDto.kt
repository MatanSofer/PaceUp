package com.example.paceup.shared.rivalengine.data

import com.example.paceup.shared.rivalengine.domain.Rival
import com.example.paceup.shared.rivalengine.domain.RivalWeeklySnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RivalDto(
    @SerialName("id") val id: String,
    @SerialName("user_a_id") val userAId: String,
    @SerialName("user_b_id") val userBId: String,
    @SerialName("status") val status: String,
    @SerialName("user_a_wins") val userAWins: Int = 0,
    @SerialName("user_b_wins") val userBWins: Int = 0,
    @SerialName("current_streak") val currentStreak: String = "tied",
    @SerialName("streak_count") val streakCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
) {
    fun toDomain(): Rival = Rival(
        id = id,
        userAId = userAId,
        userBId = userBId,
        status = status,
        userAWins = userAWins,
        userBWins = userBWins,
        currentStreak = currentStreak,
        streakCount = streakCount,
        createdAt = createdAt,
    )
}

@Serializable
data class InsertRivalDto(
    @SerialName("user_a_id") val userAId: String,
    @SerialName("user_b_id") val userBId: String,
    @SerialName("status") val status: String = "pending",
)

@Serializable
data class UpdateRivalStatusDto(
    @SerialName("status") val status: String,
)

@Serializable
data class RivalWeeklySnapshotDto(
    @SerialName("id") val id: String = "",
    @SerialName("rival_id") val rivalId: String,
    @SerialName("week_start") val weekStart: String,
    @SerialName("user_a_km") val userAKm: Float = 0f,
    @SerialName("user_b_km") val userBKm: Float = 0f,
    @SerialName("user_a_runs") val userARuns: Int = 0,
    @SerialName("user_b_runs") val userBRuns: Int = 0,
    @SerialName("user_a_best_pace") val userABestPace: Int? = null,
    @SerialName("user_b_best_pace") val userBBestPace: Int? = null,
    @SerialName("winner") val winner: String? = null,
) {
    fun toDomain(): RivalWeeklySnapshot = RivalWeeklySnapshot(
        rivalId = rivalId,
        weekStart = weekStart,
        userAKm = userAKm,
        userBKm = userBKm,
        userARuns = userARuns,
        userBRuns = userBRuns,
        userABestPace = userABestPace,
        userBBestPace = userBBestPace,
        winner = winner,
    )
}
