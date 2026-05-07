package com.example.paceup.feature.signup

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
import paceup.composeapp.generated.resources.error_auth_weak_password
import paceup.composeapp.generated.resources.error_confirm_password_required
import paceup.composeapp.generated.resources.error_email_required
import paceup.composeapp.generated.resources.error_invalid_email_format
import paceup.composeapp.generated.resources.error_password_required
import paceup.composeapp.generated.resources.error_passwords_do_not_match

data class SignUpState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null
)

sealed interface SignUpAction {
    data class OnEmailChanged(val email: String) : SignUpAction
    data class OnPasswordChanged(val password: String) : SignUpAction
    data class OnConfirmPasswordChanged(val confirmPassword: String) : SignUpAction
    data object OnSignUpClicked : SignUpAction
    data object OnGoogleSignInClicked : SignUpAction
    data object OnAppleSignInClicked : SignUpAction
    data object OnLoginClicked : SignUpAction
}

sealed interface SignUpEvent {
    /** Navigate to email verification screen after account creation. */
    data class NavigateToEmailVerification(val email: String) : SignUpEvent
    data object NavigateToLogin : SignUpEvent
}

/** ViewModel for the Sign Up screen. Handles email/password and OAuth account creation. */
class SignUpViewModel(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(
        SignUpState(email = savedStateHandle["email"] ?: "")
    )
    val state = _state.asStateFlow()

    private val _events = Channel<SignUpEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: SignUpAction) {
        when (action) {
            is SignUpAction.OnEmailChanged -> {
                savedStateHandle["email"] = action.email
                _state.update { it.copy(email = action.email, error = null) }
            }
            is SignUpAction.OnPasswordChanged -> {
                _state.update { it.copy(password = action.password, error = null) }
            }
            is SignUpAction.OnConfirmPasswordChanged -> {
                _state.update { it.copy(confirmPassword = action.confirmPassword, error = null) }
            }
            SignUpAction.OnSignUpClicked -> signUpWithEmail()
            SignUpAction.OnGoogleSignInClicked -> signInWithGoogle()
            SignUpAction.OnAppleSignInClicked -> signInWithApple()
            SignUpAction.OnLoginClicked -> viewModelScope.launch {
                _events.send(SignUpEvent.NavigateToLogin)
            }
        }
    }

    private fun signUpWithEmail() {
        val email = _state.value.email.trim()
        val password = _state.value.password
        val confirmPassword = _state.value.confirmPassword

        val validationError = validateFields(email, password, confirmPassword)
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            AppLogger.d(TAG, "signUpWithEmail: enter")
            when (val result = authRepository.signUpWithEmail(email, password)) {
                is Result.Success -> {
                    AppLogger.i(TAG, "signUpWithEmail: success user=${result.data.id}")
                    _state.update { it.copy(isLoading = false) }
                    _events.send(SignUpEvent.NavigateToEmailVerification(email))
                }
                is Result.Error -> {
                    AppLogger.w(TAG, "signUpWithEmail: failed error=${result.error}")
                    _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                }
            }
        }
    }

    private fun validateFields(email: String, password: String, confirmPassword: String): UiText? {
        if (email.isBlank()) return UiText.StringRes(Res.string.error_email_required)
        if (!emailRegex.matches(email)) return UiText.StringRes(Res.string.error_invalid_email_format)
        if (password.isBlank()) return UiText.StringRes(Res.string.error_password_required)
        if (confirmPassword.isBlank()) return UiText.StringRes(Res.string.error_confirm_password_required)
        if (password.length < 8) return UiText.StringRes(Res.string.error_auth_weak_password)
        if (password != confirmPassword) return UiText.StringRes(Res.string.error_passwords_do_not_match)
        return null
    }

    private fun signInWithGoogle() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            AppLogger.d(TAG, "signInWithGoogle: enter")
            when (val result = authRepository.signInWithGoogle()) {
                is Result.Success -> {
                    // TODO(paceup): route to StravaConnect (OAuth accounts skip email verification) — Task 2.1
                    _state.update { it.copy(isLoading = false) }
                    _events.send(SignUpEvent.NavigateToEmailVerification(result.data.email ?: ""))
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
                    // TODO(paceup): route to StravaConnect (OAuth accounts skip email verification) — Task 2.1
                    _state.update { it.copy(isLoading = false) }
                    _events.send(SignUpEvent.NavigateToEmailVerification(result.data.email ?: ""))
                }
                is Result.Error -> {
                    AppLogger.w(TAG, "signInWithApple: failed error=${result.error}")
                    _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                }
            }
        }
    }

    private companion object {
        const val TAG = "SignUpViewModel"
        val emailRegex = Regex("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")
    }
}
