package com.example.paceup.shared.runmatching.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Payload for inserting a new row into `run_participants`. */
@Serializable
internal data class JoinRunDto(
    @SerialName("run_id") val runId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("status") val status: String,
)

/** Payload for PATCH-updating only the `status` column of `run_participants`. */
@Serializable
internal data class UpdateParticipantStatusDto(
    @SerialName("status") val status: String,
)

/** Payload for creator-cancelling a run: sets status + reason on the `runs` table. */
@Serializable
internal data class CancelRunDto(
    @SerialName("status") val status: String = "cancelled",
    @SerialName("cancellation_reason") val cancellationReason: String,
)
