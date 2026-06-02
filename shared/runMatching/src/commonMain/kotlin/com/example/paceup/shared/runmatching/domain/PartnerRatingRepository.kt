package com.example.paceup.shared.runmatching.domain

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result

interface PartnerRatingRepository {
    /**
     * Returns all other participants who attended [runId] and have not yet been rated
     * by the current user in that run.
     */
    suspend fun getPartnersToRate(runId: String): Result<List<PartnerToRate>, AppError>

    /**
     * Inserts rating rows into partner_ratings.
     * [ratings] maps rated user IDs to the selected [PartnerTag] set for that user.
     * Empty tag sets are allowed (skipping a partner without tags).
     */
    suspend fun submitRatings(
        runId: String,
        ratings: Map<String, Set<PartnerTag>>,
    ): EmptyResult<AppError>

    /** Returns true if the current user has already submitted any rating for [runId]. */
    suspend fun hasRatedRun(runId: String): Result<Boolean, AppError>
}
