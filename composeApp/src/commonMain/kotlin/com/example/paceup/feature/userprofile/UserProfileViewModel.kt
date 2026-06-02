package com.example.paceup.feature.userprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.RunRepository
import com.example.paceup.shared.runmatching.domain.UserProfile
import com.example.paceup.shared.runmatching.domain.UserRepository
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserProfileState(
    val profile: UserProfile? = null,
    val recentRuns: List<Run> = emptyList(),
    val isLoading: Boolean = false,
    /** True when the viewed profile belongs to the currently authenticated user. */
    val isOwnProfile: Boolean = false,
    val error: UiText? = null,
)

sealed interface UserProfileAction {
    data object OnBackClick : UserProfileAction
    data object OnRetry : UserProfileAction
}

sealed interface UserProfileEvent {
    data object NavigateBack : UserProfileEvent
}

/** Loads a user's full profile — own or another runner's (spec §4.1). */
class UserProfileViewModel(
    private val userRepository: UserRepository,
    private val runRepository: RunRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])

    private val _state = MutableStateFlow(UserProfileState())
    val state = _state.asStateFlow()

    private val _events = Channel<UserProfileEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadProfile()
    }

    fun onAction(action: UserProfileAction) {
        when (action) {
            UserProfileAction.OnBackClick -> viewModelScope.launch {
                _events.send(UserProfileEvent.NavigateBack)
            }
            UserProfileAction.OnRetry -> loadProfile()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentUserId = (authRepository.getCurrentUser() as? Result.Success)?.data?.id

            val profileResult = userRepository.getUserProfile(userId)
            if (profileResult is Result.Error) {
                _state.update {
                    it.copy(isLoading = false, error = UiText.DynamicString("Failed to load profile"))
                }
                return@launch
            }
            val profile = (profileResult as Result.Success).data

            val recentRuns = (runRepository.getRunsForUser(userId) as? Result.Success)?.data
                ?.filter { it.status.value in listOf("completed", "in_progress") }
                ?.take(10)
                ?: emptyList()

            _state.update {
                it.copy(
                    profile = profile,
                    recentRuns = recentRuns,
                    isLoading = false,
                    isOwnProfile = currentUserId == userId,
                )
            }
        }
    }
}
