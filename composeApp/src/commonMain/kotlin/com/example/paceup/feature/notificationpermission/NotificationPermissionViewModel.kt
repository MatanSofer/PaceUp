package com.example.paceup.feature.notificationpermission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.platform.NotificationPermissionPrefs
import com.example.paceup.platform.NotificationPermissionRequester
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class NotificationPermissionState(
    val isLoading: Boolean = false
)

sealed interface NotificationPermissionAction {
    data object OnTurnOnClicked : NotificationPermissionAction
    data object OnMaybeLaterClicked : NotificationPermissionAction
}

sealed interface NotificationPermissionEvent {
    data object RequestPermission : NotificationPermissionEvent
    data object NavigateToProfileSetup : NotificationPermissionEvent
}

/**
 * Drives the notification permission onboarding step.
 * Auto-skips if notifications already granted or if user previously chose "Maybe later".
 */
class NotificationPermissionViewModel(
    private val requester: NotificationPermissionRequester,
    private val prefs: NotificationPermissionPrefs
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationPermissionState())
    val state = _state.asStateFlow()

    private val _events = Channel<NotificationPermissionEvent>()
    val events = _events.receiveAsFlow()

    init {
        if (requester.isGranted() || prefs.wasDeclined()) {
            viewModelScope.launch {
                _events.send(NotificationPermissionEvent.NavigateToProfileSetup)
            }
        }
    }

    fun onAction(action: NotificationPermissionAction) {
        when (action) {
            NotificationPermissionAction.OnTurnOnClicked -> viewModelScope.launch {
                _events.send(NotificationPermissionEvent.RequestPermission)
                _events.send(NotificationPermissionEvent.NavigateToProfileSetup)
            }
            NotificationPermissionAction.OnMaybeLaterClicked -> viewModelScope.launch {
                prefs.markDeclined()
                _events.send(NotificationPermissionEvent.NavigateToProfileSetup)
            }
        }
    }
}
