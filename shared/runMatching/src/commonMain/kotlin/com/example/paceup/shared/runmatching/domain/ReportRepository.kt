package com.example.paceup.shared.runmatching.domain

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.EmptyResult

/**
 * Parameters for submitting a report (spec §6.2).
 * For message reports, pass the sender's id in [reportedUserId] and the message content in [description].
 */
data class ReportParams(
    /** "user" | "run" | "message" */
    val reportType: String,
    val reportedUserId: String? = null,
    val reportedRunId: String? = null,
    val reason: String,
    val description: String? = null,
)

/** Submits safety reports to the admin moderation queue. Spec §6.2. */
interface ReportRepository {
    suspend fun submitReport(params: ReportParams): EmptyResult<AppError>
}
