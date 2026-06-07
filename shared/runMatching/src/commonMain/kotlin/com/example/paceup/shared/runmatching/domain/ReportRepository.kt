package com.example.paceup.shared.runmatching.domain

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.EmptyResult

/**
 * Parameters for submitting a report (spec §6.2).
 * Maps to the `reports` table columns: target_type + target_id.
 * For message reports, pass the sender's userId as [targetId] and include message text in [description].
 */
data class ReportParams(
    /** "user" | "run" | "message" — stored in reports.target_type */
    val targetType: String,
    /** UUID of the reported entity (user id, run id, or message sender id) */
    val targetId: String? = null,
    val reason: String,
    val description: String? = null,
)

/** Submits safety reports to the admin moderation queue. Spec §6.2. */
interface ReportRepository {
    suspend fun submitReport(params: ReportParams): EmptyResult<AppError>
}
