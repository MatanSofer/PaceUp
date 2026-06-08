package com.example.paceup.shared.runmatching.domain

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.EmptyResult
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

    /**
     * Returns a lightweight [UserSummary] for [userId].
     * Used when only display name, avatar, and pace zone are needed (e.g. rival cards).
     * Returns null if the user row does not exist.
     */
    suspend fun getUserSummary(userId: String): Result<UserSummary?, AppError>

    // ── Account management (spec §5.2 Account) ────────────────────────────────

    /** Returns the current signed-in user's full profile. */
    suspend fun getCurrentProfile(): Result<UserProfile, AppError>

    /** Updates [displayName] and [bio] for the current user. */
    suspend fun updateProfile(displayName: String, bio: String?): EmptyResult<AppError>

    /** Disconnects Strava: sets strava_connected=false and is_verified=false (spec §5.2). */
    suspend fun disconnectStrava(): EmptyResult<AppError>

    /** Disconnects Garmin: sets garmin_connected=false. is_verified remains if Strava active. */
    suspend fun disconnectGarmin(): EmptyResult<AppError>

    /** Calls fn_export_user_data() RPC and returns the result as a JSON string (GDPR §6.4). */
    suspend fun exportUserData(): Result<String, AppError>

    // ── Privacy settings (spec §5.2 Privacy) ─────────────────────────────────

    /** Returns the current user's privacy settings. */
    suspend fun getPrivacySettings(): Result<PrivacySettings, AppError>

    /** Persists all privacy settings for the current user in a single update. */
    suspend fun updatePrivacySettings(settings: PrivacySettings): EmptyResult<AppError>
}
