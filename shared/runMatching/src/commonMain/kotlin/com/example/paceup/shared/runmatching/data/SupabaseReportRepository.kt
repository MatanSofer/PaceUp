package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.NetworkError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.ReportParams
import com.example.paceup.shared.runmatching.domain.ReportRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "SupabaseReportRepository"
private const val TABLE = "reports"

/** Supabase-backed implementation of [ReportRepository]. Spec §6.2. */
class SupabaseReportRepository(private val client: SupabaseClient) : ReportRepository {

    override suspend fun submitReport(params: ReportParams): EmptyResult<AppError> {
        AppLogger.d(TAG, "submitReport type=${params.reportType} reason=${params.reason}")
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: return Result.Error(NetworkError.UNAUTHORIZED)
        return runCatching {
            val row = buildJsonObject {
                put("reporter_id", currentUserId)
                put("report_type", params.reportType)
                put("reason", params.reason)
                params.reportedUserId?.let { put("reported_user_id", it) }
                params.reportedRunId?.let { put("reported_run_id", it) }
                params.description?.takeIf { it.isNotBlank() }?.let { put("description", it) }
            }
            client.postgrest[TABLE].insert(row)
            AppLogger.i(TAG, "submitReport success type=${params.reportType}")
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                AppLogger.e(TAG, "submitReport failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }
}
