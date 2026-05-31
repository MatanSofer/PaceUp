package com.example.paceup.shared.notifications

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.NetworkError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "SupabaseNotificationRepository"
private const val TABLE = "users"

/** Updates `push_token` in the `users` table for the currently signed-in user. */
class SupabaseNotificationRepository(private val client: SupabaseClient) : NotificationRepository {

    override suspend fun registerPushToken(token: String): EmptyResult<AppError> {
        AppLogger.d(TAG, "registerPushToken enter")
        val userId = client.auth.currentUserOrNull()?.id
        if (userId == null) {
            AppLogger.w(TAG, "registerPushToken: no active session — skipping")
            return Result.Success(Unit)
        }
        return runCatching {
            val row = buildJsonObject {
                put("id", userId)
                put("push_token", token)
            }
            client.postgrest[TABLE].upsert(row)
            AppLogger.i(TAG, "registerPushToken success userId=$userId")
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                AppLogger.e(TAG, "registerPushToken failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }

    override suspend fun deletePushToken(): EmptyResult<AppError> {
        AppLogger.d(TAG, "deletePushToken enter")
        val userId = client.auth.currentUserOrNull()?.id
        if (userId == null) {
            AppLogger.w(TAG, "deletePushToken: no active session — skipping")
            return Result.Success(Unit)
        }
        return runCatching {
            val row = buildJsonObject {
                put("id", userId)
                put("push_token", JsonNull)
            }
            client.postgrest[TABLE].upsert(row)
            AppLogger.i(TAG, "deletePushToken success userId=$userId")
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                AppLogger.e(TAG, "deletePushToken failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }
}
