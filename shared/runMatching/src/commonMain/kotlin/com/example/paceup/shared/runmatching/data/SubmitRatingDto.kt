package com.example.paceup.shared.runmatching.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row inserted into partner_ratings for a single rated partner. */
@Serializable
internal data class SubmitRatingDto(
    @SerialName("run_id") val runId: String,
    @SerialName("rater_id") val raterId: String,
    @SerialName("rated_user_id") val ratedUserId: String,
    @SerialName("tags") val tags: List<String>,
)
