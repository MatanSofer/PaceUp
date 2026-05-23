package com.example.paceup.feature.stravaconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.auth.strava.StravaAuthRepository
import com.example.paceup.shared.auth.strava.StravaOAuthCodeStore
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.pacezone.PaceZone
import com.example.paceup.shared.pacezone.PaceZoneCalculator
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import paceup.composeapp.generated.resources.Res
import paceup.composeapp.generated.resources.error_strava_connect_failed
import paceup.composeapp.generated.resources.error_strava_fetch_failed

data class StravaConnectState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val paceZone: PaceZone? = null,
    val avgPaceDisplay: String? = null,
    val error: UiText? = null
)

sealed interface StravaConnectAction {
    data object OnConnectStravaClicked : StravaConnectAction
    data object OnSkipClicked : StravaConnectAction
    data object OnContinueClicked : StravaConnectAction
    data object OnRetryClicked : StravaConnectAction
}

sealed interface StravaConnectEvent {
    /** Open the Strava OAuth authorization page in the device browser. */
    data class LaunchOAuthUrl(val url: String) : StravaConnectEvent
    data object NavigateToLocationPermission : StravaConnectEvent
}

/** Orchestrates Strava OAuth → token exchange → activity fetch → pace zone calculation. */
class StravaConnectViewModel(
    private val stravaAuthRepository: StravaAuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StravaConnectState())
    val state = _state.asStateFlow()

    private val _events = Channel<StravaConnectEvent>()
    val events = _events.receiveAsFlow()

    init {
        observePendingOAuthCode()
    }

    fun onAction(action: StravaConnectAction) {
        when (action) {
            StravaConnectAction.OnConnectStravaClicked -> launchOAuth()
            StravaConnectAction.OnSkipClicked -> skip()
            StravaConnectAction.OnContinueClicked -> navigateForward()
            StravaConnectAction.OnRetryClicked -> {
                _state.update { it.copy(error = null) }
                launchOAuth()
            }
        }
    }

    private fun launchOAuth() {
        viewModelScope.launch {
            AppLogger.d(TAG, "launchOAuth: building OAuth URL")
            val url = stravaAuthRepository.buildOAuthUrl()
            _events.send(StravaConnectEvent.LaunchOAuthUrl(url))
        }
    }

    private fun observePendingOAuthCode() {
        viewModelScope.launch {
            StravaOAuthCodeStore.pendingCode.collect { code ->
                if (code != null) handleOAuthCode(code)
            }
        }
    }

    private suspend fun handleOAuthCode(code: String) {
        AppLogger.d(TAG, "handleOAuthCode: enter")
        _state.update { it.copy(isLoading = true, error = null) }
        StravaOAuthCodeStore.consumeCode()

        when (val tokenResult = stravaAuthRepository.exchangeCodeForToken(code)) {
            is Result.Error -> {
                AppLogger.w(TAG, "handleOAuthCode: token exchange failed error=${tokenResult.error}")
                _state.update {
                    it.copy(isLoading = false, error = UiText.StringRes(Res.string.error_strava_connect_failed))
                }
                return
            }
            is Result.Success -> {
                val token = tokenResult.data
                when (val activitiesResult = stravaAuthRepository.fetchRecentActivities(token.accessToken)) {
                    is Result.Error -> {
                        AppLogger.w(TAG, "handleOAuthCode: activities fetch failed error=${activitiesResult.error}")
                        _state.update {
                            it.copy(isLoading = false, error = UiText.StringRes(Res.string.error_strava_fetch_failed))
                        }
                    }
                    is Result.Success -> {
                        val activities = activitiesResult.data
                        val paceResult = PaceZoneCalculator.calculate(
                            activities.map { Pair(it.avgPaceSecondsPerKm, it.distanceKm) }
                        )
                        // TODO(paceup): store to Supabase users table once schema is ready (Task 3.x)
                        //   strava_connected = true
                        //   is_verified = paceResult != null
                        //   pace_zone = paceResult?.zone?.name
                        AppLogger.i(TAG, "handleOAuthCode: complete zone=${paceResult?.zone}")
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isConnected = true,
                                paceZone = paceResult?.zone,
                                avgPaceDisplay = paceResult?.avgPaceSecondsPerKm?.let { p ->
                                    PaceZoneCalculator.formatPace(p)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun skip() {
        // TODO(paceup): store strava_connected=false, is_verified=false in Supabase users table
        AppLogger.d(TAG, "skip: user skipped Strava connect")
        viewModelScope.launch { _events.send(StravaConnectEvent.NavigateToLocationPermission) }
    }

    private fun navigateForward() {
        viewModelScope.launch { _events.send(StravaConnectEvent.NavigateToLocationPermission) }
    }

    private companion object {
        const val TAG = "StravaConnectViewModel"
    }
}
