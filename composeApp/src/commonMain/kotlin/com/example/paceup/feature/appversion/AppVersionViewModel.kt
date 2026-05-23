package com.example.paceup.feature.appversion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface VersionCheckState {
    data object Loading : VersionCheckState
    data object UpToDate : VersionCheckState
    data class SoftUpdate(val message: String?) : VersionCheckState
    data class ForceUpdate(val message: String?) : VersionCheckState
}

data class AppVersionState(
    val versionCheck: VersionCheckState = VersionCheckState.Loading,
    val softUpdateDismissed: Boolean = false
)

sealed interface AppVersionAction {
    data object DismissSoftUpdate : AppVersionAction
}

/** Checks app_config on init and exposes whether a force or soft update is required. */
class AppVersionViewModel(
    private val repository: AppVersionRepository,
    private val appVersion: String
) : ViewModel() {

    private val _state = MutableStateFlow(AppVersionState())
    val state = _state.asStateFlow()

    init {
        checkVersion()
    }

    fun onAction(action: AppVersionAction) {
        when (action) {
            AppVersionAction.DismissSoftUpdate ->
                _state.update { it.copy(softUpdateDismissed = true) }
        }
    }

    private fun checkVersion() {
        viewModelScope.launch {
            AppLogger.d(TAG, "checkVersion: appVersion=$appVersion")
            when (val result = repository.checkVersion(appVersion, PLATFORM)) {
                is Result.Success -> {
                    val checkState = when (val info = result.data) {
                        null -> VersionCheckState.UpToDate
                        else -> if (info.forceUpdate) {
                            VersionCheckState.ForceUpdate(info.message)
                        } else {
                            VersionCheckState.SoftUpdate(info.message)
                        }
                    }
                    AppLogger.i(TAG, "checkVersion result: $checkState")
                    _state.update { it.copy(versionCheck = checkState) }
                }
                is Result.Error -> {
                    AppLogger.w(TAG, "checkVersion failed — treating as up-to-date: ${result.error}")
                    _state.update { it.copy(versionCheck = VersionCheckState.UpToDate) }
                }
            }
        }
    }

    private companion object {
        const val TAG = "AppVersionViewModel"
        const val PLATFORM = "android"
    }
}
