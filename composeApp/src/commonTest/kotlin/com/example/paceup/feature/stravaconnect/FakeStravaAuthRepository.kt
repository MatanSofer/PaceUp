package com.example.paceup.feature.stravaconnect

import com.example.paceup.shared.auth.strava.StravaActivityMetrics
import com.example.paceup.shared.auth.strava.StravaAuthRepository
import com.example.paceup.shared.auth.strava.StravaToken
import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.error.NetworkError
import com.example.paceup.shared.network.result.Result

class FakeStravaAuthRepository : StravaAuthRepository {

    var oauthUrl: String = "https://strava.com/oauth/authorize?client_id=test"

    var tokenResult: Result<StravaToken, AuthError> = Result.Success(
        StravaToken(
            accessToken = "fake-access-token",
            refreshToken = "fake-refresh-token",
            expiresAt = Long.MAX_VALUE,
            athleteId = 12345L
        )
    )

    var activitiesResult: Result<List<StravaActivityMetrics>, NetworkError> = Result.Success(
        listOf(
            StravaActivityMetrics(avgPaceSecondsPerKm = 300, distanceKm = 10f, date = "2025-01-01"),
            StravaActivityMetrics(avgPaceSecondsPerKm = 310, distanceKm = 8f, date = "2025-01-08")
        )
    )

    override fun buildOAuthUrl(): String = oauthUrl

    override suspend fun exchangeCodeForToken(code: String): Result<StravaToken, AuthError> =
        tokenResult

    override suspend fun fetchRecentActivities(accessToken: String): Result<List<StravaActivityMetrics>, NetworkError> =
        activitiesResult
}
