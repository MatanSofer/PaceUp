package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.UserProfile
import com.example.paceup.shared.runmatching.domain.UserRepository
import com.example.paceup.shared.runmatching.domain.UserSummary
import com.example.paceup.shared.network.error.NetworkError
import com.example.paceup.shared.network.result.EmptyResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "SupabaseUserRepository"
private const val TABLE = "users"

@Serializable
private data class ReputationTierDto(
    @SerialName("reputation_tier") val reputationTier: String? = null,
)

@Serializable
private data class UserProfileDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("pace_zone") val paceZone: String? = null,
    @SerialName("avg_pace_seconds") val avgPaceSeconds: Int? = null,
    @SerialName("weekly_mileage_avg") val weeklyMileageAvg: Float? = null,
    @SerialName("longest_run_km") val longestRunKm: Float? = null,
    @SerialName("show_up_rate") val showUpRate: Float? = null,
    @SerialName("total_paceup_runs") val totalPaceupRuns: Int? = null,
    @SerialName("unique_partners") val uniquePartners: Int? = null,
    @SerialName("reputation_tier") val reputationTier: String? = null,
    @SerialName("strava_connected") val stravaConnected: Boolean = false,
    @SerialName("garmin_connected") val garminConnected: Boolean = false,
) {
    fun toDomain() = UserProfile(
        id = id,
        displayName = displayName,
        avatarUrl = avatarUrl,
        bio = bio,
        city = city,
        paceZone = paceZone,
        avgPaceSeconds = avgPaceSeconds,
        weeklyMileageAvg = weeklyMileageAvg,
        longestRunKm = longestRunKm,
        showUpRate = showUpRate,
        totalPaceupRuns = totalPaceupRuns,
        uniquePartners = uniquePartners,
        reputationTier = reputationTier,
        stravaConnected = stravaConnected,
        garminConnected = garminConnected,
    )
}

/** Supabase-backed implementation of [UserRepository]. */
class SupabaseUserRepository(private val supabase: SupabaseClient) : UserRepository {

    override suspend fun searchUsers(query: String): Result<List<UserSummary>, AppError> {
        AppLogger.d(TAG, "searchUsers query=$query")
        return runCatching {
            supabase.postgrest[TABLE]
                .select(Columns.raw("id, display_name, avatar_url, pace_zone, show_up_rate")) {
                    filter {
                        ilike("display_name", "%$query%")
                        // Only show non-banned, non-suspended users in search results
                        eq("is_banned", false)
                        eq("is_suspended", false)
                    }
                    limit(50)
                }
                .decodeList<UserSearchDto>()
                .map { it.toDomain() }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "searchUsers failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    override suspend fun getReputationTier(userId: String): Result<String?, AppError> {
        AppLogger.d(TAG, "getReputationTier userId=$userId")
        return runCatching {
            supabase.postgrest[TABLE]
                .select(Columns.raw("reputation_tier")) {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeList<ReputationTierDto>()
                .firstOrNull()
                ?.reputationTier
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "getReputationTier failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    override suspend fun getUserSummary(userId: String): Result<UserSummary?, AppError> {
        AppLogger.d(TAG, "getUserSummary userId=$userId")
        return runCatching {
            supabase.postgrest[TABLE]
                .select(Columns.raw("id, display_name, avatar_url, pace_zone, show_up_rate")) {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeList<UserSearchDto>()
                .firstOrNull()
                ?.toDomain()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "getUserSummary failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }

    override suspend fun getUserProfile(userId: String): Result<UserProfile, AppError> {
        AppLogger.d(TAG, "getUserProfile userId=$userId")
        return runCatching {
            supabase.postgrest[TABLE]
                .select(
                    Columns.raw(
                        "id, display_name, avatar_url, bio, city, pace_zone, avg_pace_seconds, " +
                            "weekly_mileage_avg, longest_run_km, show_up_rate, total_paceup_runs, " +
                            "unique_partners, reputation_tier, strava_connected, garmin_connected"
                    )
                ) {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeList<UserProfileDto>()
                .firstOrNull()
                ?.toDomain()
                ?: throw NoSuchElementException("User not found: $userId")
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "getUserProfile failed: ${e.message}")
                val error = if (e is NoSuchElementException) RunError.NOT_FOUND else RunError.NETWORK_ERROR
                Result.Error(error)
            }
        )
    }

    // ── Account management ────────────────────────────────────────────────────

    override suspend fun getCurrentProfile(): Result<UserProfile, AppError> {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error(NetworkError.UNAUTHORIZED)
        return getUserProfile(userId)
    }

    override suspend fun updateProfile(displayName: String, bio: String?): EmptyResult<AppError> {
        AppLogger.d(TAG, "updateProfile displayName=$displayName")
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error(NetworkError.UNAUTHORIZED)
        return runCatching {
            val update = buildJsonObject {
                put("display_name", displayName)
                if (bio != null) put("bio", bio) else put("bio", JsonNull)
            }
            supabase.postgrest[TABLE].update(update) { filter { eq("id", userId) } }
            AppLogger.i(TAG, "updateProfile success")
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                AppLogger.e(TAG, "updateProfile failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }

    override suspend fun disconnectStrava(): EmptyResult<AppError> {
        AppLogger.d(TAG, "disconnectStrava")
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error(NetworkError.UNAUTHORIZED)
        return runCatching {
            val update = buildJsonObject {
                put("strava_connected", false)
                put("is_verified", false)
                put("strava_athlete_id", JsonNull)
            }
            supabase.postgrest[TABLE].update(update) { filter { eq("id", userId) } }
            AppLogger.i(TAG, "disconnectStrava success")
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                AppLogger.e(TAG, "disconnectStrava failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }

    override suspend fun disconnectGarmin(): EmptyResult<AppError> {
        AppLogger.d(TAG, "disconnectGarmin")
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error(NetworkError.UNAUTHORIZED)
        return runCatching {
            val update = buildJsonObject {
                put("garmin_connected", false)
                put("garmin_user_id", JsonNull)
            }
            supabase.postgrest[TABLE].update(update) { filter { eq("id", userId) } }
            AppLogger.i(TAG, "disconnectGarmin success")
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                AppLogger.e(TAG, "disconnectGarmin failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }

    override suspend fun exportUserData(): Result<String, AppError> {
        AppLogger.d(TAG, "exportUserData")
        return runCatching {
            supabase.postgrest.rpc("fn_export_user_data").data
        }.fold(
            onSuccess = { json ->
                AppLogger.i(TAG, "exportUserData success bytes=${json.length}")
                Result.Success(json)
            },
            onFailure = { e ->
                AppLogger.e(TAG, "exportUserData failed: ${e.message}")
                Result.Error(NetworkError.UNKNOWN)
            }
        )
    }
}
