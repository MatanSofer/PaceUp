package com.example.paceup.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.AppSettings
import com.example.paceup.shared.runmatching.domain.UserRepository
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State ─────────────────────────────────────────────────────────────────────

data class AppSettingsState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiText? = null,

    val language: String = "en",          // "en" | "he"
    val units: String = "km",             // "km" | "miles"
    val mapStyle: String = "standard",    // "standard" | "satellite"

    val appVersion: String = "",
)

// ── Actions ───────────────────────────────────────────────────────────────────

sealed interface AppSettingsAction {
    data object OnBackClick                               : AppSettingsAction
    data class OnLanguageChange(val value: String)        : AppSettingsAction
    data class OnUnitsChange(val value: String)           : AppSettingsAction
    data class OnMapStyleChange(val value: String)        : AppSettingsAction
    data object OnSave                                    : AppSettingsAction
    data object OnDismissSaveSuccess                      : AppSettingsAction
    data object OnCopyVersion                             : AppSettingsAction
}

// ── Events ────────────────────────────────────────────────────────────────────

sealed interface AppSettingsEvent {
    data object NavigateBack                : AppSettingsEvent
    data class CopyToClipboard(val text: String) : AppSettingsEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/** App settings — language, units, map style, app version. */
class AppSettingsViewModel(
    private val userRepository: UserRepository,
    private val appVersion: String,
) : ViewModel() {

    private val _state = MutableStateFlow(AppSettingsState(appVersion = appVersion))
    val state = _state.asStateFlow()

    private val _events = Channel<AppSettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadSettings()
    }

    fun onAction(action: AppSettingsAction) {
        when (action) {
            AppSettingsAction.OnBackClick -> viewModelScope.launch { _events.send(AppSettingsEvent.NavigateBack) }
            AppSettingsAction.OnCopyVersion -> viewModelScope.launch {
                _events.send(AppSettingsEvent.CopyToClipboard(_state.value.appVersion))
            }

            is AppSettingsAction.OnLanguageChange -> _state.update { it.copy(language = action.value) }
            is AppSettingsAction.OnUnitsChange    -> _state.update { it.copy(units = action.value) }
            is AppSettingsAction.OnMapStyleChange -> _state.update { it.copy(mapStyle = action.value) }

            AppSettingsAction.OnSave -> save()
            AppSettingsAction.OnDismissSaveSuccess -> _state.update { it.copy(saveSuccess = false) }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = userRepository.getAppSettings()) {
                is Result.Success -> {
                    val s = result.data
                    _state.update {
                        it.copy(isLoading = false, language = s.language, units = s.units, mapStyle = s.mapStyle)
                    }
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = UiText.DynamicString("Failed to load app settings"))
                }
            }
        }
    }

    private fun save() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val current = _state.value
            val settings = AppSettings(
                language = current.language,
                units    = current.units,
                mapStyle = current.mapStyle,
            )
            when (userRepository.updateAppSettings(settings)) {
                is Result.Success -> _state.update { it.copy(isSaving = false, saveSuccess = true) }
                is Result.Error   -> _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
