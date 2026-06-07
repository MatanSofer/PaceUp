package com.example.paceup.feature.rundetail

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.UserProfile
import com.example.paceup.shared.runmatching.domain.UserRepository
import com.example.paceup.shared.runmatching.domain.UserSummary

class FakeUserRepository : UserRepository {

    var reputationTierResult: Result<String?, AppError> = Result.Success("active")
    var userProfileResult: Result<UserProfile, AppError> = Result.Error(RunError.NOT_FOUND)

    override suspend fun searchUsers(query: String): Result<List<UserSummary>, AppError> =
        Result.Success(emptyList())

    override suspend fun getReputationTier(userId: String): Result<String?, AppError> =
        reputationTierResult

    override suspend fun getUserProfile(userId: String): Result<UserProfile, AppError> =
        userProfileResult

    override suspend fun getUserSummary(userId: String): Result<UserSummary?, AppError> =
        Result.Success(null)
}
