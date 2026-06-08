package com.example.paceup.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.PrivacySettings
import com.example.paceup.shared.runmatching.domain.UserRepository
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State ─────────────────────────────────────────────────────────────────────

data class PrivacySettingsState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiText? = null,

    val profileVisibility: String = "public",   // "public" | "friends" | "private"
    val showPaceZone: Boolean = true,
    val showRunHistory: Boolean = true,
    val showRivals: Boolean = true,
    val locationPrecision: String = "city",      // "city" | "country"
)

// ── Actions ───────────────────────────────────────────────────────────────────

sealed interface PrivacySettingsAction {
    data object OnBackClick                                   : PrivacySettingsAction
    data class OnProfileVisibilityChange(val value: String)   : PrivacySettingsAction
    data class OnShowPaceZoneChange(val value: Boolean)       : PrivacySettingsAction
    data class OnShowRunHistoryChange(val value: Boolean)     : PrivacySettingsAction
    data class OnShowRivalsChange(val value: Boolean)         : PrivacySettingsAction
    data class OnLocationPrecisionChange(val value: String)   : PrivacySettingsAction
    data object OnSave                                        : PrivacySettingsAction
    data object OnDismissSaveSuccess                          : PrivacySettingsAction
    data object OnNavigateToBlockedUsers                      : PrivacySettingsAction
}

// ── Events ────────────────────────────────────────────────────────────────────

sealed interface PrivacySettingsEvent {
    data object NavigateBack         : PrivacySettingsEvent
    data object NavigateToBlockedUsers : PrivacySettingsEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/** Privacy settings — profile visibility, toggles, location precision, blocked users. */
class PrivacySettingsViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PrivacySettingsState())
    val state = _state.asStateFlow()

    private val _events = Channel<PrivacySettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadSettings()
    }

    fun onAction(action: PrivacySettingsAction) {
        when (action) {
            PrivacySettingsAction.OnBackClick -> viewModelScope.launch { _events.send(PrivacySettingsEvent.NavigateBack) }
            PrivacySettingsAction.OnNavigateToBlockedUsers -> viewModelScope.launch { _events.send(PrivacySettingsEvent.NavigateToBlockedUsers) }

            is PrivacySettingsAction.OnProfileVisibilityChange -> _state.update { it.copy(profileVisibility = action.value) }
            is PrivacySettingsAction.OnShowPaceZoneChange      -> _state.update { it.copy(showPaceZone = action.value) }
            is PrivacySettingsAction.OnShowRunHistoryChange    -> _state.update { it.copy(showRunHistory = action.value) }
            is PrivacySettingsAction.OnShowRivalsChange        -> _state.update { it.copy(showRivals = action.value) }
            is PrivacySettingsAction.OnLocationPrecisionChange -> _state.update { it.copy(locationPrecision = action.value) }

            PrivacySettingsAction.OnSave -> save()
            PrivacySettingsAction.OnDismissSaveSuccess -> _state.update { it.copy(saveSuccess = false) }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = userRepository.getPrivacySettings()) {
                is Result.Success -> _state.update {
                    val s = result.data
                    it.copy(
                        isLoading         = false,
                        profileVisibility = s.profileVisibility,
                        showPaceZone      = s.showPaceZone,
                        showRunHistory    = s.showRunHistory,
                        showRivals        = s.showRivals,
                        locationPrecision = s.locationPrecision,
                    )
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = UiText.DynamicString("Failed to load privacy settings"))
                }
            }
        }
    }

    private fun save() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val current = _state.value
            val settings = PrivacySettings(
                profileVisibility = current.profileVisibility,
                showPaceZone      = current.showPaceZone,
                showRunHistory    = current.showRunHistory,
                showRivals        = current.showRivals,
                locationPrecision = current.locationPrecision,
            )
            when (userRepository.updatePrivacySettings(settings)) {
                is Result.Success -> _state.update { it.copy(isSaving = false, saveSuccess = true) }
                is Result.Error   -> _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
