package com.example.paceup.feature.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeState(val showTooltip: Boolean = true)

sealed interface HomeAction {
    data object OnTooltipDismissed : HomeAction
}

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnTooltipDismissed -> _state.update { it.copy(showTooltip = false) }
        }
    }
}
