package com.example.paceup.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// ── Actions ───────────────────────────────────────────────────────────────────

sealed interface SettingsAction {
    data object OnBackClick         : SettingsAction
    data object OnAccountClick      : SettingsAction
    data object OnNotificationsClick: SettingsAction
    data object OnPrivacyClick      : SettingsAction
    data object OnAppClick          : SettingsAction
}

// ── Events ────────────────────────────────────────────────────────────────────

sealed interface SettingsEvent {
    data object NavigateBack         : SettingsEvent
    data object NavigateToAccount    : SettingsEvent
    data object NavigateToNotifications: SettingsEvent
    data object NavigateToPrivacy    : SettingsEvent
    data object NavigateToApp        : SettingsEvent
}

/** Top-level settings hub — navigation only, no server calls. */
class SettingsViewModel : ViewModel() {

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: SettingsAction) {
        viewModelScope.launch {
            when (action) {
                SettingsAction.OnBackClick          -> _events.send(SettingsEvent.NavigateBack)
                SettingsAction.OnAccountClick       -> _events.send(SettingsEvent.NavigateToAccount)
                SettingsAction.OnNotificationsClick -> _events.send(SettingsEvent.NavigateToNotifications)
                SettingsAction.OnPrivacyClick       -> _events.send(SettingsEvent.NavigateToPrivacy)
                SettingsAction.OnAppClick           -> _events.send(SettingsEvent.NavigateToApp)
            }
        }
    }
}
