package com.example.paceup.feature.profilesetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.platform.ImagePicker
import com.example.paceup.platform.ImagePickerResult
import com.example.paceup.shared.auth.profile.ProfileError
import com.example.paceup.shared.auth.profile.ProfileRepository
import com.example.paceup.shared.network.result.Result
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import paceup.composeapp.generated.resources.Res
import paceup.composeapp.generated.resources.error_auth_session_expired
import paceup.composeapp.generated.resources.error_profile_avatar_upload
import paceup.composeapp.generated.resources.error_profile_name_required
import paceup.composeapp.generated.resources.error_profile_name_too_long
import paceup.composeapp.generated.resources.error_profile_name_too_short
import paceup.composeapp.generated.resources.error_profile_save_failed

data class ProfileSetupState(
    val displayName: String = "",
    val displayNameError: UiText? = null,
    val avatarBytes: ByteArray? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
)

sealed interface ProfileSetupAction {
    data class OnDisplayNameChanged(val value: String) : ProfileSetupAction
    data object OnPickAvatarClicked : ProfileSetupAction
    data object OnContinueClicked : ProfileSetupAction
}

sealed interface ProfileSetupEvent {
    data object NavigateToHome : ProfileSetupEvent
}

class ProfileSetupViewModel(
    private val profileRepository: ProfileRepository,
    private val imagePicker: ImagePicker,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSetupState())
    val state = _state.asStateFlow()

    private val _events = Channel<ProfileSetupEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            ImagePickerResult.pendingBytes.collect { bytes ->
                bytes ?: return@collect
                _state.update { it.copy(avatarBytes = bytes) }
                ImagePickerResult.clear()
            }
        }
    }

    fun onAction(action: ProfileSetupAction) {
        when (action) {
            is ProfileSetupAction.OnDisplayNameChanged ->
                _state.update { it.copy(displayName = action.value, displayNameError = null) }
            ProfileSetupAction.OnPickAvatarClicked -> imagePicker.launch()
            ProfileSetupAction.OnContinueClicked -> saveProfile()
        }
    }

    private fun saveProfile() {
        val name = _state.value.displayName.trim()
        if (name.isBlank()) {
            _state.update {
                it.copy(displayNameError = UiText.StringRes(Res.string.error_profile_name_required))
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = profileRepository.saveProfile(
                displayName = name,
                avatarBytes = _state.value.avatarBytes,
            )) {
                is Result.Success -> _events.send(ProfileSetupEvent.NavigateToHome)
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUiText())
                }
            }
        }
    }

    private fun ProfileError.toUiText(): UiText = UiText.StringRes(
        when (this) {
            ProfileError.DISPLAY_NAME_BLANK,
            ProfileError.DISPLAY_NAME_TOO_SHORT -> Res.string.error_profile_name_too_short
            ProfileError.DISPLAY_NAME_TOO_LONG -> Res.string.error_profile_name_too_long
            ProfileError.AVATAR_UPLOAD_FAILED -> Res.string.error_profile_avatar_upload
            ProfileError.SAVE_FAILED -> Res.string.error_profile_save_failed
            ProfileError.NOT_AUTHENTICATED -> Res.string.error_auth_session_expired
        }
    )
}
