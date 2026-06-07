package com.example.paceup.shared.rivalengine.data

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.rivalengine.domain.Rival
import com.example.paceup.shared.rivalengine.domain.RivalRepository
import com.example.paceup.shared.rivalengine.domain.RivalWeeklySnapshot
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.mapNotNull

private const val TAG = "SupabaseRivalRepository"
private const val TABLE = "rivals"
private const val SNAPSHOTS_TABLE = "rival_weekly_snapshots"

/** Supabase-backed implementation of [RivalRepository]. */
class SupabaseRivalRepository(private val supabase: SupabaseClient) : RivalRepository {

    override suspend fun sendRivalRequest(targetUserId: String): Result<Unit, AppError> {
        AppLogger.d(TAG, "sendRivalRequest targetUserId=$targetUserId")
        val currentUser = supabase.auth.currentUserOrNull()
            ?: return Result.Error(AuthError.UNAUTHORIZED)
        return runCatching {
            supabase.postgrest[TABLE].insert(
                InsertRivalDto(userAId = currentUser.id, userBId = targetUserId)
            )
        }.fold(
            onSuccess = {
                AppLogger.i(TAG, "sendRivalRequest success target=$targetUserId")
                Result.Success(Unit)
            },
            onFailure = { e ->
                AppLogger.e(TAG, "sendRivalRequest failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    override suspend fun acceptRivalRequest(rivalId: String): Result<Unit, AppError> {
        AppLogger.d(TAG, "acceptRivalRequest rivalId=$rivalId")
        return runCatching {
            supabase.postgrest[TABLE]
                .update(UpdateRivalStatusDto(status = "active")) {
                    filter { eq("id", rivalId) }
                }
        }.fold(
            onSuccess = {
                AppLogger.i(TAG, "acceptRivalRequest success rivalId=$rivalId")
                Result.Success(Unit)
            },
            onFailure = { e ->
                AppLogger.e(TAG, "acceptRivalRequest failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    override suspend fun declineRivalRequest(rivalId: String): Result<Unit, AppError> {
        AppLogger.d(TAG, "declineRivalRequest rivalId=$rivalId")
        return runCatching {
            supabase.postgrest[TABLE]
                .update(UpdateRivalStatusDto(status = "declined")) {
                    filter { eq("id", rivalId) }
                }
        }.fold(
            onSuccess = {
                AppLogger.i(TAG, "declineRivalRequest success rivalId=$rivalId")
                Result.Success(Unit)
            },
            onFailure = { e ->
                AppLogger.e(TAG, "declineRivalRequest failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    override suspend fun getRivals(): Result<List<Rival>, AppError> {
        AppLogger.d(TAG, "getRivals")
        val currentUser = supabase.auth.currentUserOrNull()
            ?: return Result.Error(AuthError.UNAUTHORIZED)
        val userId = currentUser.id

        return runCatching {
            // Fetch rivals from both directions (current user as initiator or recipient).
            // Two small queries are used because max rivals is 5 and PostgREST OR filtering
            // across two columns is awkward in supabase-kt.
            val asA = supabase.postgrest[TABLE]
                .select {
                    filter {
                        eq("user_a_id", userId)
                        neq("status", "declined")
                        neq("status", "ended")
                    }
                }
                .decodeList<RivalDto>()

            val asB = supabase.postgrest[TABLE]
                .select {
                    filter {
                        eq("user_b_id", userId)
                        neq("status", "declined")
                        neq("status", "ended")
                    }
                }
                .decodeList<RivalDto>()

            (asA + asB).distinctBy { it.id }.map { it.toDomain() }
        }.fold(
            onSuccess = { rivals ->
                AppLogger.i(TAG, "getRivals success count=${rivals.size}")
                Result.Success(rivals)
            },
            onFailure = { e ->
                AppLogger.e(TAG, "getRivals failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    override fun observeRivalRequest(): Flow<Rival> = channelFlow {
        val currentUser = supabase.auth.currentUserOrNull() ?: return@channelFlow
        val userId = currentUser.id

        val channel = supabase.channel("rivals:$userId")
        // Subscribe to all INSERTs on rivals; filter for this user as user_b_id in the Flow
        // because the Realtime filter DSL is private in supabase-kt 3.1.4.
        val changeFlow: Flow<PostgresAction.Insert> = channel.postgresChangeFlow(schema = "public") {
            table = TABLE
        }
        channel.subscribe()
        AppLogger.i(TAG, "Realtime rival channel subscribed userId=$userId")
        changeFlow
            .mapNotNull { action ->
                runCatching { action.decodeRecord<RivalDto>() }
                    .onFailure { AppLogger.e(TAG, "decode rival record failed: ${it.message}") }
                    .getOrNull()
            }
            .collect { dto ->
                // Emit only incoming requests addressed to this user.
                if (dto.userBId == userId && dto.status == "pending") {
                    AppLogger.i(TAG, "observeRivalRequest: new request rivalId=${dto.id} from=${dto.userAId}")
                    send(dto.toDomain())
                }
            }
        supabase.realtime.removeChannel(channel)
    }

    override suspend fun getLatestSnapshot(rivalId: String): Result<RivalWeeklySnapshot?, AppError> {
        AppLogger.d(TAG, "getLatestSnapshot rivalId=$rivalId")
        return runCatching {
            val rows = supabase.postgrest[SNAPSHOTS_TABLE]
                .select {
                    filter { eq("rival_id", rivalId) }
                    order("week_start", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<RivalWeeklySnapshotDto>()
            rows.firstOrNull()?.toDomain()
        }.fold(
            onSuccess = { snapshot ->
                AppLogger.i(TAG, "getLatestSnapshot success rivalId=$rivalId found=${snapshot != null}")
                Result.Success(snapshot)
            },
            onFailure = { e ->
                AppLogger.e(TAG, "getLatestSnapshot failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }
}
