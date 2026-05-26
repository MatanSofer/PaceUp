package com.example.paceup.shared.runmatching.domain

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.Result

/** Repository for user discovery — searching runners by display name. */
interface UserRepository {

    /**
     * Search users by display_name using trigram matching (pg_trgm).
     * Results capped at 50 per spec §10.6.
     */
    suspend fun searchUsers(query: String): Result<List<UserSummary>, AppError>
}
