package com.example.paceup.shared.auth.strava

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton bridge for the Strava OAuth callback code.
 *
 * The platform entry-point (MainActivity on Android, AppDelegate on iOS) calls [submitCode]
 * when the `paceup://strava/callback?code=XXX` deep link fires. The [StravaConnectViewModel]
 * collects [pendingCode] and calls [consumeCode] once processed to prevent re-processing.
 */
object StravaOAuthCodeStore {

    private val _pendingCode = MutableStateFlow<String?>(null)

    /** Emits a non-null code when a Strava OAuth redirect has been received. */
    val pendingCode: StateFlow<String?> = _pendingCode.asStateFlow()

    /** Called by the platform layer when the deep-link redirect delivers a code. */
    fun submitCode(code: String) {
        _pendingCode.value = code
    }

    /** Called by the ViewModel after consuming the code to prevent double-processing. */
    fun consumeCode() {
        _pendingCode.value = null
    }
}
