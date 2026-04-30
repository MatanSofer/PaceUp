package com.example.paceup.feature.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.Result
import com.example.paceup.ui.UiText
import com.example.paceup.ui.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import paceup.composeapp.generated.resources.Res
import paceup.composeapp.generated.resources.error_auth_invalid_credentials

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null
)

sealed interface LoginAction {
    data class OnEmailChanged(val email: String) : LoginAction
    data class OnPasswordChanged(val password: String) : LoginAction
    data object OnLoginClicked : LoginAction
    data object OnGoogleSignInClicked : LoginAction
    data object OnAppleSignInClicked : LoginAction
    data object OnSignUpClicked : LoginAction
}

sealed interface LoginEvent {
    /** Navigate to Strava connect after sign-in. TODO(paceup): route to HomeRoute when user already completed onboarding. */
    data object NavigateToStravaConnect : LoginEvent
    data object NavigateToSignUp : LoginEvent
}

/** ViewModel for the Login screen. Handles email/password and OAuth sign-in. */
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(
        LoginState(email = savedStateHandle["email"] ?: "")
    )
    val state = _state.asStateFlow()

    private val _events = Channel<LoginEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnEmailChanged -> {
                savedStateHandle["email"] = action.email
                _state.update { it.copy(email = action.email, error = null) }
            }
            is LoginAction.OnPasswordChanged -> {
                _state.update { it.copy(password = action.password, error = null) }
            }
            LoginAction.OnLoginClicked -> signInWithEmail()
            LoginAction.OnGoogleSignInClicked -> signInWithGoogle()
            LoginAction.OnAppleSignInClicked -> signInWithApple()
            LoginAction.OnSignUpClicked -> viewModelScope.launch {
                _events.send(LoginEvent.NavigateToSignUp)
            }
        }
    }

    private fun signInWithEmail() {
        val email = _state.value.email.trim()
        val password = _state.value.password
        if (email.isBlank() || password.isBlank()) {
            _state.update {
                it.copy(error = UiText.StringRes(Res.string.error_auth_invalid_credentials))
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            AppLogger.d(TAG, "signInWithEmail: enter")
            when (val result = authRepository.signInWithEmail(email, password)) {
                is Result.Success -> {
                    AppLogger.i(TAG, "signInWithEmail: success user=${result.data.id}")
                    _state.update { it.copy(isLoading = false) }
                    _events.send(LoginEvent.NavigateToStravaConnect)
                }
                is Result.Error -> {
                    AppLogger.w(TAG, "signInWithEmail: failed error=${result.error}")
                    _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                }
            }
        }
    }

    private fun signInWithGoogle() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            AppLogger.d(TAG, "signInWithGoogle: enter")
            when (val result = authRepository.signInWithGoogle()) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(LoginEvent.NavigateToStravaConnect)
                }
                is Result.Error -> {
                    AppLogger.w(TAG, "signInWithGoogle: failed error=${result.error}")
                    _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                }
            }
        }
    }

    private fun signInWithApple() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            AppLogger.d(TAG, "signInWithApple: enter")
            when (val result = authRepository.signInWithApple()) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(LoginEvent.NavigateToStravaConnect)
                }
                is Result.Error -> {
                    AppLogger.w(TAG, "signInWithApple: failed error=${result.error}")
                    _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                }
            }
        }
    }

    private companion object {
        const val TAG = "LoginViewModel"
    }
}
