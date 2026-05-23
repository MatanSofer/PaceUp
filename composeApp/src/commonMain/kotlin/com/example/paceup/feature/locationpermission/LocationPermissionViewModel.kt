package com.example.paceup.feature.locationpermission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class LocationPermissionState(
    val isLoading: Boolean = false
)

sealed interface LocationPermissionAction {
    data object OnAllowClicked : LocationPermissionAction
    data object OnSkipClicked : LocationPermissionAction
}

sealed interface LocationPermissionEvent {
    data object RequestPermission : LocationPermissionEvent
    data object NavigateToNotifications : LocationPermissionEvent
}

/** Drives the location permission onboarding step. Navigation always proceeds regardless of result. */
class LocationPermissionViewModel : ViewModel() {

    private val _state = MutableStateFlow(LocationPermissionState())
    val state = _state.asStateFlow()

    private val _events = Channel<LocationPermissionEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: LocationPermissionAction) {
        when (action) {
            LocationPermissionAction.OnAllowClicked -> viewModelScope.launch {
                _events.send(LocationPermissionEvent.RequestPermission)
                _events.send(LocationPermissionEvent.NavigateToNotifications)
            }
            LocationPermissionAction.OnSkipClicked -> viewModelScope.launch {
                _events.send(LocationPermissionEvent.NavigateToNotifications)
            }
        }
    }
}
