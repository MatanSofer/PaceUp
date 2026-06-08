package com.example.paceup.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.UserProfile
import com.example.paceup.shared.runmatching.domain.UserRepository
import com.example.paceup.ui.UiText
import com.example.paceup.ui.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State ─────────────────────────────────────────────────────────────────────

data class AccountSettingsState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val error: UiText? = null,

    // Profile edit
    val editName: String = "",
    val editBio: String = "",
    val isSavingProfile: Boolean = false,
    val profileSaveSuccess: Boolean = false,

    // Email change
    val newEmail: String = "",
    val isSavingEmail: Boolean = false,
    val emailChangeSuccess: Boolean = false,
    val emailError: UiText? = null,

    // Password change
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSavingPassword: Boolean = false,
    val passwordChangeSuccess: Boolean = false,
    val passwordError: UiText? = null,

    // Connected apps
    val isDisconnectingStrava: Boolean = false,
    val isDisconnectingGarmin: Boolean = false,

    // Danger zone
    val showDeleteDialog: Boolean = false,
    val deleteConfirmText: String = "",
    val isDeletingAccount: Boolean = false,
    val isExportingData: Boolean = false,
    val exportedJson: String? = null,
)

// ── Actions ───────────────────────────────────────────────────────────────────

sealed interface AccountSettingsAction {
    data object OnBackClick : AccountSettingsAction

    // Profile
    data class OnNameChange(val value: String)     : AccountSettingsAction
    data class OnBioChange(val value: String)      : AccountSettingsAction
    data object OnSaveProfile                      : AccountSettingsAction
    data object OnDismissProfileSuccess            : AccountSettingsAction

    // Email
    data class OnNewEmailChange(val value: String) : AccountSettingsAction
    data object OnSaveEmail                        : AccountSettingsAction
    data object OnDismissEmailResult               : AccountSettingsAction

    // Password
    data class OnNewPasswordChange(val value: String)     : AccountSettingsAction
    data class OnConfirmPasswordChange(val value: String) : AccountSettingsAction
    data object OnSavePassword                            : AccountSettingsAction
    data object OnDismissPasswordResult                   : AccountSettingsAction

    // Connected apps
    data object OnDisconnectStrava : AccountSettingsAction
    data object OnDisconnectGarmin : AccountSettingsAction

    // Danger zone
    data object OnDeleteAccountClick             : AccountSettingsAction
    data class OnDeleteConfirmTextChange(val value: String) : AccountSettingsAction
    data object OnConfirmDeleteAccount           : AccountSettingsAction
    data object OnDismissDeleteDialog            : AccountSettingsAction
    data object OnExportDataClick                : AccountSettingsAction
    data object OnDismissExportedData            : AccountSettingsAction
}

// ── Events ────────────────────────────────────────────────────────────────────

sealed interface AccountSettingsEvent {
    data object NavigateBack    : AccountSettingsEvent
    data object AccountDeleted  : AccountSettingsEvent
    data class ShareJson(val json: String) : AccountSettingsEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

/** Account settings hub — profile edit, email/password change, connected apps, GDPR ops. */
class AccountSettingsViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountSettingsState())
    val state = _state.asStateFlow()

    private val _events = Channel<AccountSettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadProfile()
    }

    fun onAction(action: AccountSettingsAction) {
        when (action) {
            AccountSettingsAction.OnBackClick            -> viewModelScope.launch { _events.send(AccountSettingsEvent.NavigateBack) }

            is AccountSettingsAction.OnNameChange        -> _state.update { it.copy(editName = action.value) }
            is AccountSettingsAction.OnBioChange         -> _state.update { it.copy(editBio = action.value) }
            AccountSettingsAction.OnSaveProfile          -> saveProfile()
            AccountSettingsAction.OnDismissProfileSuccess -> _state.update { it.copy(profileSaveSuccess = false) }

            is AccountSettingsAction.OnNewEmailChange    -> _state.update { it.copy(newEmail = action.value) }
            AccountSettingsAction.OnSaveEmail            -> saveEmail()
            AccountSettingsAction.OnDismissEmailResult   -> _state.update { it.copy(emailChangeSuccess = false, emailError = null) }

            is AccountSettingsAction.OnNewPasswordChange    -> _state.update { it.copy(newPassword = action.value) }
            is AccountSettingsAction.OnConfirmPasswordChange -> _state.update { it.copy(confirmPassword = action.value) }
            AccountSettingsAction.OnSavePassword            -> savePassword()
            AccountSettingsAction.OnDismissPasswordResult   -> _state.update { it.copy(passwordChangeSuccess = false, passwordError = null) }

            AccountSettingsAction.OnDisconnectStrava -> disconnectStrava()
            AccountSettingsAction.OnDisconnectGarmin -> disconnectGarmin()

            AccountSettingsAction.OnDeleteAccountClick -> _state.update { it.copy(showDeleteDialog = true) }
            is AccountSettingsAction.OnDeleteConfirmTextChange -> _state.update { it.copy(deleteConfirmText = action.value) }
            AccountSettingsAction.OnConfirmDeleteAccount -> deleteAccount()
            AccountSettingsAction.OnDismissDeleteDialog -> _state.update { it.copy(showDeleteDialog = false, deleteConfirmText = "") }
            AccountSettingsAction.OnExportDataClick -> exportData()
            AccountSettingsAction.OnDismissExportedData -> _state.update { it.copy(exportedJson = null) }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = userRepository.getCurrentProfile()) {
                is Result.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        profile = result.data,
                        editName = result.data.displayName,
                        editBio = result.data.bio ?: "",
                    )
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = UiText.DynamicString("Failed to load profile"))
                }
            }
        }
    }

    private fun saveProfile() {
        val name = _state.value.editName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSavingProfile = true) }
            val bio = _state.value.editBio.trim().ifBlank { null }
            when (userRepository.updateProfile(name, bio)) {
                is Result.Success -> {
                    _state.update { it.copy(isSavingProfile = false, profileSaveSuccess = true, profile = it.profile?.copy(displayName = name, bio = bio)) }
                }
                is Result.Error -> _state.update { it.copy(isSavingProfile = false) }
            }
        }
    }

    private fun saveEmail() {
        val email = _state.value.newEmail.trim()
        if (email.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSavingEmail = true, emailError = null) }
            when (val result = authRepository.updateEmail(email)) {
                is Result.Success -> _state.update { it.copy(isSavingEmail = false, emailChangeSuccess = true, newEmail = "") }
                is Result.Error -> _state.update { it.copy(isSavingEmail = false, emailError = result.error.toUiText()) }
            }
        }
    }

    private fun savePassword() {
        val pw = _state.value.newPassword
        val confirm = _state.value.confirmPassword
        if (pw.isBlank()) return
        if (pw != confirm) {
            _state.update { it.copy(passwordError = UiText.DynamicString("Passwords do not match")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSavingPassword = true, passwordError = null) }
            when (val result = authRepository.updatePassword(pw)) {
                is Result.Success -> _state.update { it.copy(isSavingPassword = false, passwordChangeSuccess = true, newPassword = "", confirmPassword = "") }
                is Result.Error -> _state.update { it.copy(isSavingPassword = false, passwordError = result.error.toUiText()) }
            }
        }
    }

    private fun disconnectStrava() {
        viewModelScope.launch {
            _state.update { it.copy(isDisconnectingStrava = true) }
            when (userRepository.disconnectStrava()) {
                is Result.Success -> _state.update {
                    it.copy(
                        isDisconnectingStrava = false,
                        profile = it.profile?.copy(stravaConnected = false),
                    )
                }
                is Result.Error -> _state.update { it.copy(isDisconnectingStrava = false) }
            }
        }
    }

    private fun disconnectGarmin() {
        viewModelScope.launch {
            _state.update { it.copy(isDisconnectingGarmin = true) }
            when (userRepository.disconnectGarmin()) {
                is Result.Success -> _state.update {
                    it.copy(
                        isDisconnectingGarmin = false,
                        profile = it.profile?.copy(garminConnected = false),
                    )
                }
                is Result.Error -> _state.update { it.copy(isDisconnectingGarmin = false) }
            }
        }
    }

    private fun deleteAccount() {
        if (_state.value.deleteConfirmText != "DELETE") return
        viewModelScope.launch {
            _state.update { it.copy(isDeletingAccount = true) }
            when (authRepository.deleteAccount()) {
                is Result.Success -> {
                    _state.update { it.copy(isDeletingAccount = false, showDeleteDialog = false) }
                    _events.send(AccountSettingsEvent.AccountDeleted)
                }
                is Result.Error -> _state.update { it.copy(isDeletingAccount = false) }
            }
        }
    }

    private fun exportData() {
        viewModelScope.launch {
            _state.update { it.copy(isExportingData = true) }
            when (val result = userRepository.exportUserData()) {
                is Result.Success -> {
                    _state.update { it.copy(isExportingData = false) }
                    _events.send(AccountSettingsEvent.ShareJson(result.data))
                }
                is Result.Error -> _state.update { it.copy(isExportingData = false) }
            }
        }
    }
}
