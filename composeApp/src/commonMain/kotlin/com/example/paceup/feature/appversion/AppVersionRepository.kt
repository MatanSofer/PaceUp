package com.example.paceup.feature.appversion

import com.example.paceup.shared.network.error.NetworkError
import com.example.paceup.shared.network.result.Result
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape from the app_config Supabase table. */
@Serializable
data class AppConfigDto(
    @SerialName("min_version") val minVersion: String,
    @SerialName("force_update") val forceUpdate: Boolean,
    @SerialName("update_message") val updateMessage: String? = null
)

/** Non-null only when the current version is below min_version. */
data class UpdateInfo(val forceUpdate: Boolean, val message: String?)

interface AppVersionRepository {
    /** Returns [UpdateInfo] when an update is needed, null when up-to-date. */
    suspend fun checkVersion(currentVersion: String, platform: String): Result<UpdateInfo?, NetworkError>
}

/** Returns true when [current] is strictly below [minimum] using semver ordering. */
fun isVersionBelow(current: String, minimum: String): Boolean {
    fun parse(v: String) = v.trim().split(".").map { it.toIntOrNull() ?: 0 }
    val curr = parse(current)
    val min = parse(minimum)
    val length = maxOf(curr.size, min.size)
    repeat(length) { i ->
        val c = curr.getOrElse(i) { 0 }
        val m = min.getOrElse(i) { 0 }
        if (c < m) return true
        if (c > m) return false
    }
    return false
}
