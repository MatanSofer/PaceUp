package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.runmatching.domain.RunDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shape returned when selecting `runs(*)` via run_participants join. */
@Serializable
internal data class RunParticipantWithRunDto(
    @SerialName("runs") val run: RunDto? = null,
)
