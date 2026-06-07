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

    override suspend fun getPreferences(): Result<NotificationPreferences, AppError> {
        AppLogger.d(TAG, "getPreferences enter")
        val userId = client.auth.currentUserOrNull()?.id ?: return Result.Error(NetworkError.UNAUTHORIZED)
        return runCatching {
            val dto = client.postgrest[TABLE]
                .select(io.github.jan.supabase.postgrest.query.Columns.raw(
                    "notif_run_reminders, notif_join_requests, notif_rival_nudges, " +
                    "notif_rival_summary, notif_new_runs, notif_partner_ratings, notif_marketing"
                )) {
                    filter { eq("id", userId) }
                }
                .decodeSingle<NotificationPreferencesDto>()
            AppLogger.i(TAG, "getPreferences success userId=$userId")
            dto.toDomain()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "getPreferences failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }

    override suspend fun updatePreferences(prefs: NotificationPreferences): EmptyResult<AppError> {
        AppLogger.d(TAG, "updatePreferences enter")
        val userId = client.auth.currentUserOrNull()?.id ?: return Result.Error(NetworkError.UNAUTHORIZED)
        return runCatching {
            val row = buildJsonObject {
                put("id", userId)
                put("notif_run_reminders",   prefs.runReminders)
                put("notif_join_requests",    prefs.joinRequests)
                put("notif_rival_nudges",     prefs.rivalNudges)
                put("notif_rival_summary",    prefs.rivalSummary)
                put("notif_new_runs",         prefs.newRuns)
                put("notif_partner_ratings",  prefs.partnerRatings)
                put("notif_marketing",        prefs.marketing)
            }
            client.postgrest[TABLE].upsert(row)
            AppLogger.i(TAG, "updatePreferences success userId=$userId")
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                AppLogger.e(TAG, "updatePreferences failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }
}
