package com.example.paceup.shared.auth.strava

import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.error.NetworkError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** [StravaAuthRepository] backed by Ktor HTTP calls to the Strava API. */
class KtorStravaAuthRepository : StravaAuthRepository {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override fun buildOAuthUrl(): String {
        // TODO(paceup): replace CLIENT_ID with real value from Strava developer portal
        return "$OAUTH_BASE_URL/authorize" +
            "?client_id=$STRAVA_CLIENT_ID" +
            "&redirect_uri=${encodeUrl(REDIRECT_URI)}" +
            "&response_type=code" +
            "&approval_prompt=auto" +
            "&scope=$SCOPE"
    }

    override suspend fun exchangeCodeForToken(code: String): Result<StravaToken, AuthError> {
        AppLogger.d(TAG, "exchangeCodeForToken enter")
        return try {
            val dto = httpClient.post("$OAUTH_BASE_URL/token") {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("client_id", STRAVA_CLIENT_ID)
                    put("client_secret", STRAVA_CLIENT_SECRET)
                    put("code", code)
                    put("grant_type", "authorization_code")
                })
            }.body<StravaTokenDto>()
            AppLogger.i(TAG, "exchangeCodeForToken success athleteId=${dto.athlete.id}")
            Result.Success(dto.toDomain())
        } catch (e: Exception) {
            AppLogger.e(TAG, "exchangeCodeForToken failed: ${e.message}")
            Result.Error(AuthError.OAUTH_FAILED)
        }
    }

    override suspend fun fetchRecentActivities(accessToken: String): Result<List<StravaActivityMetrics>, NetworkError> {
        AppLogger.d(TAG, "fetchRecentActivities enter")
        return try {
            val afterEpoch = ninetyDaysAgoEpoch()
            val activities = httpClient.get("$API_BASE_URL/athlete/activities") {
                header("Authorization", "Bearer $accessToken")
                parameter("after", afterEpoch)
                parameter("per_page", 200)
            }.body<List<StravaActivityDto>>()

            val metrics = activities
                .filter { it.type == "Run" && it.distanceMeters >= MIN_DISTANCE_METERS }
                .mapNotNull { it.toMetrics() }

            AppLogger.i(TAG, "fetchRecentActivities: total=${activities.size} runs=${metrics.size}")
            Result.Success(metrics)
        } catch (e: Exception) {
            AppLogger.e(TAG, "fetchRecentActivities failed: ${e.message}")
            Result.Error(NetworkError.UNKNOWN)
        }
    }

    private companion object {
        const val TAG = "KtorStravaAuthRepository"
        const val OAUTH_BASE_URL = "https://www.strava.com/oauth"
        const val API_BASE_URL = "https://www.strava.com/api/v3"
        const val REDIRECT_URI = "paceup://strava/callback"
        const val SCOPE = "activity:read_all,profile:read_all"
        const val MIN_DISTANCE_METERS = 3000f // spec: only runs > 3km included in pace zone calc

        // TODO(paceup): move to local.properties / build config before production
        const val STRAVA_CLIENT_ID = "YOUR_STRAVA_CLIENT_ID"
        const val STRAVA_CLIENT_SECRET = "YOUR_STRAVA_CLIENT_SECRET"
    }
}

/** Returns a simple URL-encoding of common characters in a redirect URI. */
private fun encodeUrl(url: String): String =
    url.replace(":", "%3A").replace("/", "%2F")

/** Returns the Unix epoch for 90 days ago. */
expect fun ninetyDaysAgoEpoch(): Long
