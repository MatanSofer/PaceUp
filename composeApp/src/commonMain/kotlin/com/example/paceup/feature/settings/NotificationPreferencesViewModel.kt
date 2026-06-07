package com.example.paceup.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.notifications.NotificationPreferences
import com.example.paceup.shared.notifications.NotificationRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationPreferencesState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val prefs: NotificationPreferences = NotificationPreferences(),
    val saveError: Boolean = false,
)

sealed interface NotificationPreferencesAction {
    data object OnBackClick : NotificationPreferencesAction
    data class OnToggleRunReminders(val enabled: Boolean) : NotificationPreferencesAction
    data class OnToggleJoinRequests(val enabled: Boolean) : NotificationPreferencesAction
    data class OnToggleRivalNudges(val enabled: Boolean) : NotificationPreferencesAction
    data class OnToggleRivalSummary(val enabled: Boolean) : NotificationPreferencesAction
    data class OnToggleNewRuns(val enabled: Boolean) : NotificationPreferencesAction
    data class OnTogglePartnerRatings(val enabled: Boolean) : NotificationPreferencesAction
    data class OnToggleMarketing(val enabled: Boolean) : NotificationPreferencesAction
}

sealed interface NotificationPreferencesEvent {
    data object NavigateBack : NotificationPreferencesEvent
}

/** ViewModel for the Notification Preferences settings screen (spec §5.2). */
class NotificationPreferencesViewModel(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationPreferencesState())
    val state = _state.asStateFlow()

    private val _events = Channel<NotificationPreferencesEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadPreferences()
    }

    fun onAction(action: NotificationPreferencesAction) {
        when (action) {
            is NotificationPreferencesAction.OnBackClick            -> viewModelScope.launch { _events.send(NotificationPreferencesEvent.NavigateBack) }
            is NotificationPreferencesAction.OnToggleRunReminders   -> updatePref { it.copy(runReminders = action.enabled) }
            is NotificationPreferencesAction.OnToggleJoinRequests   -> updatePref { it.copy(joinRequests = action.enabled) }
            is NotificationPreferencesAction.OnToggleRivalNudges    -> updatePref { it.copy(rivalNudges = action.enabled) }
            is NotificationPreferencesAction.OnToggleRivalSummary   -> updatePref { it.copy(rivalSummary = action.enabled) }
            is NotificationPreferencesAction.OnToggleNewRuns        -> updatePref { it.copy(newRuns = action.enabled) }
            is NotificationPreferencesAction.OnTogglePartnerRatings -> updatePref { it.copy(partnerRatings = action.enabled) }
            is NotificationPreferencesAction.OnToggleMarketing      -> updatePref { it.copy(marketing = action.enabled) }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = notificationRepository.getPreferences()
            _state.update { s ->
                when (result) {
                    is Result.Success -> s.copy(isLoading = false, prefs = result.data)
                    is Result.Error   -> s.copy(isLoading = false)
                }
            }
        }
    }

    private fun updatePref(transform: (NotificationPreferences) -> NotificationPreferences) {
        val updated = transform(_state.value.prefs)
        _state.update { it.copy(prefs = updated, isSaving = true, saveError = false) }
        viewModelScope.launch {
            val result = notificationRepository.updatePreferences(updated)
            _state.update { s ->
                s.copy(isSaving = false, saveError = result is Result.Error)
            }
        }
    }
}
