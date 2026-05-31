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

    /**
     * Inserts a new run into Supabase and returns the created [Run] with its server-assigned ID.
     * The caller is responsible for passing the authenticated user's ID as [CreateRunParams.creatorId].
     */
    suspend fun createRun(params: CreateRunParams): Result<Run, AppError>

    /** Full-text search by city, neighbourhood, or route name with optional filters. */
    suspend fun searchRuns(
        query: String,
        filters: RunFilters = RunFilters(),
    ): Result<List<Run>, AppError>

    /**
     * Returns accepted participants for a run, each with basic profile data
     * for rendering participant avatars with pace zone rings (SPEC.md §4.4).
     */
    suspend fun getRunParticipants(runId: String): Result<List<RunParticipant>, AppError>

    /**
     * Emits [RunStatus] updates for a specific run in real-time.
     * Caller is responsible for cancelling the returned [Flow].
     */
    fun observeRunStatus(runId: String): Flow<RunStatus>
}
