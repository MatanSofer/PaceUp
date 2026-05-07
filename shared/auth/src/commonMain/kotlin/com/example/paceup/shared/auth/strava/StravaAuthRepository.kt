package com.example.paceup.shared.auth.strava

import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.error.NetworkError
import com.example.paceup.shared.network.result.Result

/** Data layer for Strava OAuth and activity access. */
interface StravaAuthRepository {

    /**
     * Builds the Strava OAuth authorization URL.
     * Open this URL in a browser; Strava will redirect to the app's deep link with a code.
     */
    fun buildOAuthUrl(): String

    /**
     * Exchanges a Strava authorization [code] (from the OAuth redirect) for an access token.
     * @param code The `code` query parameter from the Strava redirect URI.
     */
    suspend fun exchangeCodeForToken(code: String): Result<StravaToken, AuthError>

    /**
     * Fetches the athlete's recent activities (last 90 days, runs only) using [accessToken].
     * Returns only derived metrics — raw data is never propagated (spec §7.1).
     */
    suspend fun fetchRecentActivities(accessToken: String): Result<List<StravaActivityMetrics>, NetworkError>
}
