package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.runmatching.domain.PartnerToRate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shape of a run_participants row joined with the users profile via PostgREST FK. */
@Serializable
internal data class PartnerToRateDto(
    @SerialName("user_id") val userId: String,
    @SerialName("users") val user: UserSummaryDto? = null,
)

internal fun PartnerToRateDto.toDomain() = PartnerToRate(
    userId = userId,
    displayName = user?.displayName ?: "Runner",
    avatarUrl = user?.avatarUrl,
    paceZone = user?.paceZone,
)
