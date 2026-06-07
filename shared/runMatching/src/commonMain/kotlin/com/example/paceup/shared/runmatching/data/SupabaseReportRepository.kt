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
        AppLogger.d(TAG, "submitReport type=${params.targetType} reason=${params.reason}")
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: return Result.Error(NetworkError.UNAUTHORIZED)
        return runCatching {
            val row = buildJsonObject {
                put("reporter_id", currentUserId)
                put("target_type", params.targetType)
                put("reason", params.reason)
                params.targetId?.let { put("target_id", it) }
                params.description?.takeIf { it.isNotBlank() }?.let { put("description", it) }
            }
            client.postgrest[TABLE].insert(row)
            AppLogger.i(TAG, "submitReport success type=${params.targetType}")
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                AppLogger.e(TAG, "submitReport failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }
}
