package com.example.paceup.shared.auth.profile

import com.example.paceup.shared.auth.strava.StravaToken
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.asEmptyResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "SupabaseProfileRepository"
private const val DISPLAY_NAME_MIN = 2
private const val DISPLAY_NAME_MAX = 30
private const val AVATAR_BUCKET = "avatars"

/** Saves profile data (display name + optional avatar) to Supabase. */
class SupabaseProfileRepository(private val supabase: SupabaseClient) : ProfileRepository {

    override suspend fun saveProfile(
        displayName: String,
        avatarBytes: ByteArray?,
    ): EmptyResult<ProfileError> {
        val trimmed = displayName.trim()

        if (trimmed.isBlank()) return com.example.paceup.shared.network.result.Result.Error(ProfileError.DISPLAY_NAME_BLANK)
        if (trimmed.length < DISPLAY_NAME_MIN) return com.example.paceup.shared.network.result.Result.Error(ProfileError.DISPLAY_NAME_TOO_SHORT)
        if (trimmed.length > DISPLAY_NAME_MAX) return com.example.paceup.shared.network.result.Result.Error(ProfileError.DISPLAY_NAME_TOO_LONG)

        val userId = supabase.auth.currentSessionOrNull()?.user?.id
            ?: return com.example.paceup.shared.network.result.Result.Error(ProfileError.NOT_AUTHENTICATED)

        AppLogger.i(TAG, "saveProfile userId=$userId")

        var avatarPath: String? = null
        if (avatarBytes != null) {
            runCatching {
                val path = "$userId/avatar.jpg"
                supabase.storage[AVATAR_BUCKET].upload(path, avatarBytes) { upsert = true }
                avatarPath = path
                AppLogger.i(TAG, "avatar uploaded → $path")
            }.onFailure { e ->
                AppLogger.e(TAG, "avatar upload failed: ${e.message}")
                return com.example.paceup.shared.network.result.Result.Error(ProfileError.AVATAR_UPLOAD_FAILED)
            }
        }

        return runCatching {
            val row = buildJsonObject {
                put("id", userId)
                put("display_name", trimmed)
                avatarPath?.let { put("avatar_url", it) }
            }
            supabase.postgrest["users"].upsert(row)
            AppLogger.i(TAG, "profile saved")
        }.fold(
            onSuccess = { com.example.paceup.shared.network.result.Result.Success(Unit).asEmptyResult() },
            onFailure = { e ->
                AppLogger.e(TAG, "profile save failed: ${e.message}")
                com.example.paceup.shared.network.result.Result.Error(ProfileError.SAVE_FAILED)
            }
        )
    }

    override suspend fun saveStravaConnection(
        token: StravaToken,
        isVerified: Boolean,
        paceZone: String?,
        avgPaceSeconds: Int?,
        weeklyMileageAvgKm: Float?,
    ): EmptyResult<ProfileError> {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id
            ?: return com.example.paceup.shared.network.result.Result.Error(ProfileError.NOT_AUTHENTICATED)

        AppLogger.i(TAG, "saveStravaConnection userId=$userId isVerified=$isVerified zone=$paceZone")

        return runCatching {
            // 1. Upsert OAuth tokens into dedicated table (service_role reads for edge functions).
            val tokenRow = buildJsonObject {
                put("user_id", userId)
                put("access_token", token.accessToken)
                put("refresh_token", token.refreshToken)
                put("expires_at", token.expiresAt)
            }
            supabase.postgrest["user_strava_tokens"].upsert(tokenRow)

            // 2. Update public.users with Strava profile + pace zone data.
            val userRow = buildJsonObject {
                put("id", userId)
                put("strava_connected", true)
                put("is_verified", isVerified)
                put("strava_athlete_id", token.athleteId.toString())
                paceZone?.let { put("pace_zone", it) }
                avgPaceSeconds?.let { put("avg_pace_seconds", it) }
                weeklyMileageAvgKm?.let { put("weekly_mileage_avg", it) }
            }
            supabase.postgrest["users"].upsert(userRow)

            AppLogger.i(TAG, "saveStravaConnection success")
        }.fold(
            onSuccess = { com.example.paceup.shared.network.result.Result.Success(Unit).asEmptyResult() },
            onFailure = { e ->
                AppLogger.e(TAG, "saveStravaConnection failed: ${e.message}")
                com.example.paceup.shared.network.result.Result.Error(ProfileError.SAVE_FAILED)
            }
        )
    }
}
