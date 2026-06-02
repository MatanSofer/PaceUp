package com.example.paceup.shared.auth.profile

import com.example.paceup.shared.auth.strava.StravaToken
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

    /**
     * Persists Strava OAuth tokens and updates the user's profile with verified pace data.
     * Tokens are stored in a dedicated table; pace data is written to the users table.
     * Required for server-side attendance verification (spec §7.1, §8.4 run_attendance_verify).
     *
     * @param token Strava OAuth token from the successful authorization code exchange.
     * @param isVerified Whether the user has enough activity data to be considered verified.
     * @param paceZone Calculated pace zone string ("A"–"E"), or null if unverifiable.
     * @param avgPaceSeconds Calculated average pace in sec/km, or null if unverifiable.
     * @param weeklyMileageAvgKm Calculated weekly mileage average, or null if unverifiable.
     */
    suspend fun saveStravaConnection(
        token: StravaToken,
        isVerified: Boolean,
        paceZone: String?,
        avgPaceSeconds: Int?,
        weeklyMileageAvgKm: Float?,
    ): EmptyResult<ProfileError>
}
