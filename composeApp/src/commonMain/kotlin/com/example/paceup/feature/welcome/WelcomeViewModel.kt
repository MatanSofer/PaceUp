package com.example.paceup.feature.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class WelcomeState(val placeholder: Unit = Unit)

sealed interface WelcomeAction {
    data object OnGetStartedClick : WelcomeAction
}

sealed interface WelcomeEvent {
    data object NavigateToLogin : WelcomeEvent
}

class WelcomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(WelcomeState())
    val state = _state.asStateFlow()

    private val _events = Channel<WelcomeEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: WelcomeAction) {
        when (action) {
            WelcomeAction.OnGetStartedClick -> viewModelScope.launch {
                _events.send(WelcomeEvent.NavigateToLogin)
            }
        }
    }
}
