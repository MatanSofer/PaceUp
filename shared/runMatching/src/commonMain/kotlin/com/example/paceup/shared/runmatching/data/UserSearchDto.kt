package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.runmatching.domain.UserSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shape returned when selecting from the users table for search purposes. */
@Serializable
internal data class UserSearchDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("pace_zone") val paceZone: String? = null,
    @SerialName("show_up_rate") val showUpRate: Float? = null,
) {
    fun toDomain() = UserSummary(
        id = id,
        displayName = displayName ?: "Runner",
        avatarUrl = avatarUrl,
        paceZone = paceZone,
        showUpRate = showUpRate,
    )
}
