package com.example.paceup.shared.rivalengine.domain

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository for rival connections and live request observation (spec §4.5).
 * All functions require an active Supabase session.
 */
interface RivalRepository {

    /**
     * Sends a rival request to [targetUserId].
     * Inserts a rivals row with status=pending. Subject to max-5-rivals limit (spec §4.5).
     */
    suspend fun sendRivalRequest(targetUserId: String): Result<Unit, AppError>

    /**
     * Accepts an incoming rival request identified by [rivalId].
     * Updates rivals.status from "pending" to "active".
     */
    suspend fun acceptRivalRequest(rivalId: String): Result<Unit, AppError>

    /**
     * Declines an incoming rival request identified by [rivalId].
     * Updates rivals.status to "declined".
     */
    suspend fun declineRivalRequest(rivalId: String): Result<Unit, AppError>

    /**
     * Returns all current and pending rival connections for the authenticated user.
     * Excludes declined and ended relationships.
     */
    suspend fun getRivals(): Result<List<Rival>, AppError>

    /**
     * Emits [Rival] values in real-time whenever another user sends the
     * authenticated user a new rival request (status=pending). Uses Supabase Realtime.
     * Caller is responsible for cancelling the returned [Flow].
     */
    fun observeRivalRequest(): Flow<Rival>

    /**
     * Returns the latest weekly snapshot for [rivalId], or null if none exists yet.
     * Used by the rival dashboard screen (Task 7.2).
     */
    suspend fun getLatestSnapshot(rivalId: String): Result<RivalWeeklySnapshot?, AppError>
}
