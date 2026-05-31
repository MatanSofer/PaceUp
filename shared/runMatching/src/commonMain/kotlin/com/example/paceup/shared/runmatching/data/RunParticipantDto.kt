package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.runmatching.domain.RunParticipant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shape returned when selecting run_participants joined with users. */
@Serializable
internal data class RunParticipantDto(
    @SerialName("user_id") val userId: String,
    @SerialName("status") val status: String,
    @SerialName("users") val user: UserSummaryDto? = null,
)

/** Public profile fields embedded from the users table via PostgREST FK join. */
@Serializable
internal data class UserSummaryDto(
    @SerialName("id") val id: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("pace_zone") val paceZone: String? = null,
    @SerialName("show_up_rate") val showUpRate: Float? = null,
)

internal fun RunParticipantDto.toDomain() = RunParticipant(
    userId = userId,
    displayName = user?.displayName ?: "Runner",
    avatarUrl = user?.avatarUrl,
    paceZone = user?.paceZone,
    showUpRate = user?.showUpRate,
    status = status,
)
