package com.example.paceup.shared.runmatching.domain

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.Result
import kotlinx.coroutines.flow.Flow

/** Repository for fetching and observing runs from Supabase. */
interface RunRepository {

    /** Returns all upcoming runs within [radiusKm] of the given coordinates. */
    suspend fun getRunsNearLocation(
        lat: Double,
        lng: Double,
        radiusKm: Double,
        filters: RunFilters = RunFilters(),
    ): Result<List<Run>, AppError>

    /** Returns a single run by its ID. */
    suspend fun getRunById(runId: String): Result<Run, AppError>

    /** Returns all runs where [userId] is a participant or creator. */
    suspend fun getRunsForUser(userId: String): Result<List<Run>, AppError>

    /** Full-text search by city, neighbourhood, or route name with optional filters. */
    suspend fun searchRuns(
        query: String,
        filters: RunFilters = RunFilters(),
    ): Result<List<Run>, AppError>

    /**
     * Emits [RunStatus] updates for a specific run in real-time.
     * Caller is responsible for cancelling the returned [Flow].
     */
    fun observeRunStatus(runId: String): Flow<RunStatus>
}
