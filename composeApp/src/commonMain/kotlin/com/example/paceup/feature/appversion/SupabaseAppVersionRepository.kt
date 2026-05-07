package com.example.paceup.feature.appversion

import com.example.paceup.shared.network.error.NetworkError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerializationException

/** [AppVersionRepository] backed by the Supabase app_config table. */
class SupabaseAppVersionRepository(private val client: SupabaseClient) : AppVersionRepository {

    override suspend fun checkVersion(
        currentVersion: String,
        platform: String
    ): Result<UpdateInfo?, NetworkError> {
        AppLogger.d(TAG, "checkVersion enter: currentVersion=$currentVersion platform=$platform")
        return try {
            val config = client.from("app_config")
                .select { filter { eq("platform", platform) } }
                .decodeSingle<AppConfigDto>()

            val needsUpdate = isVersionBelow(currentVersion, config.minVersion)
            AppLogger.i(TAG, "checkVersion: minVersion=${config.minVersion} needsUpdate=$needsUpdate force=${config.forceUpdate}")

            val result = if (needsUpdate) UpdateInfo(config.forceUpdate, config.updateMessage) else null
            Result.Success(result)
        } catch (e: SerializationException) {
            AppLogger.e(TAG, "checkVersion serialization error: ${e.message}")
            Result.Error(NetworkError.SERIALIZATION)
        } catch (e: Exception) {
            AppLogger.e(TAG, "checkVersion failed: ${e.message}")
            Result.Error(NetworkError.UNKNOWN)
        }
    }

    private companion object {
        const val TAG = "AppVersionRepository"
    }
}
