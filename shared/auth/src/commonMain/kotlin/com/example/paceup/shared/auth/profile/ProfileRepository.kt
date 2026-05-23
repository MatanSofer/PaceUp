package com.example.paceup.shared.auth.profile

import com.example.paceup.shared.network.result.EmptyResult

interface ProfileRepository {
    /**
     * Saves the user's display name and optional avatar to Supabase.
     * If [avatarBytes] is non-null, uploads to Storage first, then upserts the path into users table.
     * Always upserts display_name regardless of avatar.
     */
    suspend fun saveProfile(
        displayName: String,
        avatarBytes: ByteArray?,
    ): EmptyResult<ProfileError>
}
