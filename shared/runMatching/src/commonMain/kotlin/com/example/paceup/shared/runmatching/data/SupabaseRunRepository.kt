package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.RunDto
import com.example.paceup.shared.runmatching.domain.RunFilters
import com.example.paceup.shared.runmatching.domain.RunParticipant
import com.example.paceup.shared.runmatching.domain.RunRepository
import com.example.paceup.shared.runmatching.domain.RunStatus
import com.example.paceup.shared.runmatching.domain.toDomain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "SupabaseRunRepository"
private const val TABLE = "runs"

/**
 * Supabase-backed implementation of [RunRepository].
 * All geo-proximity filtering is done client-side because PostgREST does not expose
 * PostGIS functions via the REST API — a Supabase Edge Function will own geo queries
 * in a later task. For now, results are fetched for a city and filtered in-memory.
 *
 * TODO(paceup): replace in-memory proximity filter with Edge Function call (Task 3.x).
 */
class SupabaseRunRepository(private val supabase: SupabaseClient) : RunRepository {

    override suspend fun getRunsNearLocation(
        lat: Double,
        lng: Double,
        radiusKm: Double,
        filters: RunFilters,
    ): Result<List<Run>, AppError> {
        AppLogger.d(TAG, "getRunsNearLocation lat=$lat lng=$lng radiusKm=$radiusKm")
        return runCatching {
            val rows = supabase.postgrest[TABLE]
                .select(Columns.ALL) {
                    filter {
                        // Only upcoming runs in open/full status
                        isIn("status", listOf("open", "full"))
                    }
                    limit(200)
                }
                .decodeList<RunDto>()

            rows.map { it.toDomain() }
                .filter { run -> haversineKm(lat, lng, run.meetingLat, run.meetingLng) <= radiusKm }
                .applyFilters(filters)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "getRunsNearLocation failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    override suspend fun getRunById(runId: String): Result<Run, AppError> {
        AppLogger.d(TAG, "getRunById runId=$runId")
        return runCatching {
            supabase.postgrest[TABLE]
                .select(Columns.ALL) {
                    filter { eq("id", runId) }
                    limit(1)
                }
                .decodeSingle<RunDto>()
                .toDomain()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "getRunById failed: ${e.message}")
                val error = if (e.message?.contains("404") == true || e.message?.contains("no rows") == true)
                    RunError.NOT_FOUND else RunError.NETWORK_ERROR
                Result.Error(error)
            }
        )
    }

    override suspend fun getRunsForUser(userId: String): Result<List<Run>, AppError> {
        AppLogger.d(TAG, "getRunsForUser userId=$userId")
        return runCatching {
            // Fetch runs the user created
            val created = supabase.postgrest[TABLE]
                .select(Columns.ALL) {
                    filter { eq("creator_id", userId) }
                    order("scheduled_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(100)
                }
                .decodeList<RunDto>()
                .map { it.toDomain() }

            // Fetch runs the user joined via run_participants
            val joined = supabase.postgrest["run_participants"]
                .select(Columns.raw("runs(*)")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<RunParticipantWithRunDto>()
                .mapNotNull { it.run?.toDomain() }

            (created + joined).distinctBy { it.id }
                .sortedByDescending { it.scheduledAt }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "getRunsForUser failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    override suspend fun searchRuns(
        query: String,
        filters: RunFilters,
    ): Result<List<Run>, AppError> {
        AppLogger.d(TAG, "searchRuns query=$query")
        return runCatching {
            val rows = supabase.postgrest[TABLE]
                .select(Columns.ALL) {
                    filter {
                        isIn("status", listOf("open", "full"))
                        if (query.isNotBlank()) {
                            // PostgREST ilike for partial matching on city or meeting_address
                            or {
                                ilike("city", "%$query%")
                                ilike("meeting_address", "%$query%")
                                ilike("title", "%$query%")
                            }
                        }
                    }
                    limit(100)
                }
                .decodeList<RunDto>()

            rows.map { it.toDomain() }.applyFilters(filters)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "searchRuns failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    override suspend fun getRunParticipants(runId: String): Result<List<RunParticipant>, AppError> {
        AppLogger.d(TAG, "getRunParticipants runId=$runId")
        return runCatching {
            supabase.postgrest["run_participants"]
                .select(Columns.raw("user_id, status, users(id, display_name, avatar_url, pace_zone, show_up_rate)")) {
                    filter {
                        eq("run_id", runId)
                        eq("status", "accepted")
                    }
                }
                .decodeList<RunParticipantDto>()
                .map { it.toDomain() }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "getRunParticipants failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    /**
     * Emits [RunStatus] updates by polling the run row.
     * TODO(paceup): replace with Supabase Realtime subscription in Task shared/realtime.
     */
    override fun observeRunStatus(runId: String): Flow<RunStatus> = flow {
        val result = getRunById(runId)
        if (result is Result.Success) emit(result.data.status)
    }

    // --- helpers ---

    private fun List<Run>.applyFilters(filters: RunFilters): List<Run> {
        return filter { run ->
            (filters.paceMinSec == null || run.paceMaxSec >= filters.paceMinSec) &&
                (filters.paceMaxSec == null || run.paceMinSec <= filters.paceMaxSec) &&
                (filters.modes.isEmpty() || run.mode in filters.modes) &&
                (filters.verifiedOnly == null || run.verifiedOnly == filters.verifiedOnly) &&
                (filters.afterDate == null || run.scheduledAt >= filters.afterDate) &&
                (filters.beforeDate == null || run.scheduledAt <= filters.beforeDate) &&
                (filters.minDistanceKm == null || (run.distanceKm != null && run.distanceKm >= filters.minDistanceKm)) &&
                (filters.openJoinOnly != true || run.joinMode == "open") &&
                (filters.recurringOnly != true || run.isRecurring)
        }
    }

    /**
     * Haversine distance between two coordinates in kilometres.
     * Used for client-side proximity filtering until Edge Function geo queries are ready.
     */
    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) *
            kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2).let { it * it }
        return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }
}
