package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.PartnerRatingRepository
import com.example.paceup.shared.runmatching.domain.PartnerTag
import com.example.paceup.shared.runmatching.domain.PartnerToRate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TAG = "SupabasePartnerRatingRepository"
private const val TABLE_PARTICIPANTS = "run_participants"
private const val TABLE_RATINGS = "partner_ratings"

/** Supabase-backed implementation of [PartnerRatingRepository]. */
class SupabasePartnerRatingRepository(private val supabase: SupabaseClient) : PartnerRatingRepository {

    override suspend fun getPartnersToRate(runId: String): Result<List<PartnerToRate>, AppError> {
        AppLogger.d(TAG, "getPartnersToRate runId=$runId")
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error(AuthError.UNAUTHORIZED)

        return runCatching {
            // Attended participants in this run, excluding the current user
            val attended = supabase.postgrest[TABLE_PARTICIPANTS]
                .select(Columns.raw("user_id, users(display_name, avatar_url, pace_zone, reputation_tier)")) {
                    filter {
                        eq("run_id", runId)
                        eq("status", "attended")
                        neq("user_id", userId)
                    }
                }
                .decodeList<PartnerToRateDto>()

            if (attended.isEmpty()) return@runCatching emptyList<PartnerToRate>()

            // Partners this user has already rated in this run
            val alreadyRated = supabase.postgrest[TABLE_RATINGS]
                .select(Columns.raw("rated_user_id")) {
                    filter {
                        eq("run_id", runId)
                        eq("rater_id", userId)
                    }
                }
                .decodeList<RatedUserIdDto>()
                .map { it.ratedUserId }
                .toSet()

            AppLogger.d(TAG, "getPartnersToRate attended=${attended.size} alreadyRated=${alreadyRated.size}")
            attended.filter { it.userId !in alreadyRated }.map { it.toDomain() }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "getPartnersToRate failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            },
        )
    }

    override suspend fun submitRatings(
        runId: String,
        ratings: Map<String, Set<PartnerTag>>,
    ): EmptyResult<AppError> {
        AppLogger.d(TAG, "submitRatings runId=$runId count=${ratings.size}")
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error(AuthError.UNAUTHORIZED)

        if (ratings.isEmpty()) return Result.Success(Unit)

        return runCatching {
            val rows = ratings.map { (ratedUserId, tags) ->
                SubmitRatingDto(
                    runId = runId,
                    raterId = userId,
                    ratedUserId = ratedUserId,
                    tags = tags.map { it.value },
                )
            }
            supabase.postgrest[TABLE_RATINGS].insert(rows)
        }.fold(
            onSuccess = {
                AppLogger.i(TAG, "submitRatings success runId=$runId userId=$userId")
                Result.Success(Unit)
            },
            onFailure = { e ->
                AppLogger.e(TAG, "submitRatings failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            },
        )
    }

    override suspend fun hasRatedRun(runId: String): Result<Boolean, AppError> {
        AppLogger.d(TAG, "hasRatedRun runId=$runId")
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error(AuthError.UNAUTHORIZED)

        return runCatching {
            val rows = supabase.postgrest[TABLE_RATINGS]
                .select(Columns.raw("id")) {
                    filter {
                        eq("run_id", runId)
                        eq("rater_id", userId)
                    }
                    limit(1)
                }
                .decodeList<RatingIdDto>()
            rows.isNotEmpty()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "hasRatedRun failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            },
        )
    }
}

@Serializable
private data class RatedUserIdDto(@SerialName("rated_user_id") val ratedUserId: String)

@Serializable
private data class RatingIdDto(@SerialName("id") val id: String)
