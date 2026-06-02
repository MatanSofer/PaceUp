package com.example.paceup.feature.partnerrating

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.PartnerRatingRepository
import com.example.paceup.shared.runmatching.domain.PartnerTag
import com.example.paceup.shared.runmatching.domain.PartnerToRate

class FakePartnerRatingRepository : PartnerRatingRepository {

    var partnersToRate: List<PartnerToRate> = listOf(
        PartnerToRate("user-2", "Alice", null, "B"),
        PartnerToRate("user-3", "Bob", null, "C"),
    )
    var getPartnersResult: Result<List<PartnerToRate>, AppError>? = null  // null → use partnersToRate list
    var hasRatedResult: Result<Boolean, AppError> = Result.Success(false)
    var submitResult: EmptyResult<AppError> = Result.Success(Unit)

    var getPartnersCallCount = 0
    var submitCallCount = 0
    var lastSubmittedRatings: Map<String, Set<PartnerTag>>? = null

    override suspend fun getPartnersToRate(runId: String): Result<List<PartnerToRate>, AppError> {
        getPartnersCallCount++
        return getPartnersResult ?: Result.Success(partnersToRate)
    }

    override suspend fun submitRatings(
        runId: String,
        ratings: Map<String, Set<PartnerTag>>,
    ): EmptyResult<AppError> {
        submitCallCount++
        lastSubmittedRatings = ratings
        return submitResult
    }

    override suspend fun hasRatedRun(runId: String): Result<Boolean, AppError> = hasRatedResult
}
