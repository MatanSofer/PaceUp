package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.ChatMessage
import com.example.paceup.shared.runmatching.domain.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull

private const val TAG = "SupabaseChatRepository"
private const val TABLE = "run_chat_messages"
private const val MAX_HISTORY = 100

/** Supabase-backed implementation of [ChatRepository]. */
class SupabaseChatRepository(private val supabase: SupabaseClient) : ChatRepository {

    override suspend fun getRecentMessages(runId: String): Result<List<ChatMessage>, AppError> {
        AppLogger.d(TAG, "getRecentMessages runId=$runId")
        return runCatching {
            supabase.postgrest[TABLE]
                .select(Columns.raw("id, run_id, user_id, content, created_at, users(display_name)")) {
                    filter { eq("run_id", runId) }
                    order("created_at", Order.ASCENDING)
                    limit(MAX_HISTORY.toLong())
                }
                .decodeList<ChatMessageDto>()
                .map { it.toDomain() }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "getRecentMessages failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    override fun observeNewMessages(runId: String): Flow<ChatMessage> = channelFlow {
        val channel = supabase.channel("chat:$runId")
        // Subscribe to all inserts on the table; filter by runId in the Flow pipeline
        // because the filter DSL property is not accessible in supabase-kt 3.1.4.
        val changeFlow: Flow<PostgresAction.Insert> = channel.postgresChangeFlow(schema = "public") {
            table = TABLE
        }
        channel.subscribe()
        AppLogger.i(TAG, "Realtime channel subscribed for runId=$runId")
        changeFlow
            .mapNotNull { action ->
                runCatching { action.decodeRecord<ChatMessageRealtimeDto>() }
                    .onFailure { AppLogger.e(TAG, "decode realtime message failed: ${it.message}") }
                    .getOrNull()
            }
            .filter { it.runId == runId }
            .collect { send(it.toDomain()) }
        supabase.realtime.removeChannel(channel)
    }

    override suspend fun sendMessage(runId: String, content: String): Result<Unit, AppError> {
        AppLogger.d(TAG, "sendMessage runId=$runId contentLen=${content.length}")
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error(AuthError.UNAUTHORIZED)
        return runCatching {
            supabase.postgrest[TABLE].insert(
                SendMessageDto(runId = runId, userId = userId, content = content)
            )
        }.fold(
            onSuccess = {
                AppLogger.i(TAG, "sendMessage success runId=$runId userId=$userId")
                Result.Success(Unit)
            },
            onFailure = { e ->
                AppLogger.e(TAG, "sendMessage failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }
}
