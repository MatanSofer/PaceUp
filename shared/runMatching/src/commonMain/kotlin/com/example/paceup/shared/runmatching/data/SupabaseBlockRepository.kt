package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.NetworkError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.BlockRepository
import com.example.paceup.shared.runmatching.domain.UserSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "SupabaseBlockRepository"

@Serializable
private data class BlockRowDto(@SerialName("blocked_id") val blockedId: String)
private const val TABLE = "user_blocks"

/** Supabase-backed implementation of [BlockRepository]. Spec §6.1. */
class SupabaseBlockRepository(private val client: SupabaseClient) : BlockRepository {

    override suspend fun blockUser(targetUserId: String): EmptyResult<AppError> {
        AppLogger.d(TAG, "blockUser target=$targetUserId")
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: return Result.Error(NetworkError.UNAUTHORIZED)
        return runCatching {
            val row = buildJsonObject {
                put("blocker_id", currentUserId)
                put("blocked_id", targetUserId)
            }
            client.postgrest[TABLE].upsert(row)
            AppLogger.i(TAG, "blockUser success target=$targetUserId")
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                AppLogger.e(TAG, "blockUser failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }

    override suspend fun unblockUser(targetUserId: String): EmptyResult<AppError> {
        AppLogger.d(TAG, "unblockUser target=$targetUserId")
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: return Result.Error(NetworkError.UNAUTHORIZED)
        return runCatching {
            client.postgrest[TABLE].delete {
                filter {
                    eq("blocker_id", currentUserId)
                    eq("blocked_id", targetUserId)
                }
            }
            AppLogger.i(TAG, "unblockUser success target=$targetUserId")
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                AppLogger.e(TAG, "unblockUser failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }

    override suspend fun isBlockedByMe(targetUserId: String): Result<Boolean, AppError> {
        AppLogger.d(TAG, "isBlockedByMe target=$targetUserId")
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: return Result.Success(false)
        return runCatching {
            val rows = client.postgrest[TABLE]
                .select(Columns.raw("blocked_id")) {
                    filter {
                        eq("blocker_id", currentUserId)
                        eq("blocked_id", targetUserId)
                    }
                    limit(1)
                }
                .decodeList<BlockRowDto>()
            rows.isNotEmpty()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "isBlockedByMe failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }

    override suspend fun getBlockedUsers(): Result<List<UserSummary>, AppError> {
        AppLogger.d(TAG, "getBlockedUsers enter")
        return runCatching {
            client.postgrest
                .rpc("fn_get_blocked_users")
                .decodeList<UserSearchDto>()
                .map { it.toDomain() }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "getBlockedUsers failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }
}
