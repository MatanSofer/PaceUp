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

    /**
     * Returns the reputation_tier for [userId] (new_runner / active / trusted / pacer_eligible).
     * Used client-side to gate join actions before hitting the server (spec §4.4).
     * Returns null if the user has no Supabase profile row yet.
     */
    suspend fun getReputationTier(userId: String): Result<String?, AppError>

    /**
     * Returns the full [UserProfile] for [userId] (spec §4.1).
     * Used to render the profile screen for both the current user and other runners.
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile, AppError>
}
