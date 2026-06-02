package com.example.paceup.shared.runmatching.domain

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.Result
import kotlinx.coroutines.flow.Flow

/** Repository for run group chat messages (spec §4.6, §8.3). */
interface ChatRepository {

    /**
     * Returns the most recent messages for [runId], ordered oldest-first.
     * Used for initial screen load before the Realtime subscription catches up.
     */
    suspend fun getRecentMessages(runId: String): Result<List<ChatMessage>, AppError>

    /**
     * Emits every new message inserted for [runId] via Supabase Realtime.
     * The caller must cancel the flow to unsubscribe the Realtime channel.
     */
    fun observeNewMessages(runId: String): Flow<ChatMessage>

    /** Inserts a new message as the currently authenticated user. */
    suspend fun sendMessage(runId: String, content: String): Result<Unit, AppError>
}
