package com.example.paceup.feature.userprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.BlockRepository
import com.example.paceup.shared.runmatching.domain.ReportParams
import com.example.paceup.shared.runmatching.domain.ReportRepository
import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.RunRepository
import com.example.paceup.shared.runmatching.domain.UserProfile
import com.example.paceup.shared.runmatching.domain.UserRepository
import com.example.paceup.feature.report.ReportTarget
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
    /** True when the current user has blocked this profile. */
    val isBlockedByMe: Boolean = false,
    /** True while a block/unblock network call is in flight. */
    val isBlockLoading: Boolean = false,
    /** True when the block confirmation dialog should be shown. */
    val showBlockConfirm: Boolean = false,
    /** Non-null when the report dialog is open. */
    val reportTarget: ReportTarget? = null,
    val isReportSubmitting: Boolean = false,
    val isReportSuccess: Boolean = false,
)

sealed interface UserProfileAction {
    data object OnBackClick : UserProfileAction
    data object OnRetry : UserProfileAction
    /** Tapping the Block button — opens the confirmation dialog. */
    data object OnBlockClick : UserProfileAction
    /** User confirmed the block in the dialog. */
    data object OnConfirmBlock : UserProfileAction
    /** User dismissed the block dialog without confirming. */
    data object OnDismissBlockDialog : UserProfileAction
    /** Tapping Unblock button — unblocks immediately. */
    data object OnUnblockClick : UserProfileAction
    /** Opens the report dialog for this user. */
    data object OnReportUserClick : UserProfileAction
    /** User submitted the report form with the selected reason and optional description. */
    data class OnSubmitReport(val reason: String, val description: String) : UserProfileAction
    /** Dismisses the report dialog (and clears success state). */
    data object OnDismissReport : UserProfileAction
}

sealed interface UserProfileEvent {
    data object NavigateBack : UserProfileEvent
}

/** Loads a user's full profile — own or another runner's (spec §4.1). Supports blocking and reporting (spec §6.1–6.2). */
class UserProfileViewModel(
    private val userRepository: UserRepository,
    private val runRepository: RunRepository,
    private val authRepository: AuthRepository,
    private val blockRepository: BlockRepository,
    private val reportRepository: ReportRepository,
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
            UserProfileAction.OnBlockClick -> _state.update { it.copy(showBlockConfirm = true) }
            UserProfileAction.OnDismissBlockDialog -> _state.update { it.copy(showBlockConfirm = false) }
            UserProfileAction.OnConfirmBlock -> blockUser()
            UserProfileAction.OnUnblockClick -> unblockUser()
            UserProfileAction.OnReportUserClick -> {
                val name = _state.value.profile?.displayName ?: ""
                _state.update { it.copy(reportTarget = ReportTarget.User(userId, name)) }
            }
            is UserProfileAction.OnSubmitReport -> submitReport(action.reason, action.description)
            UserProfileAction.OnDismissReport -> _state.update {
                it.copy(reportTarget = null, isReportSuccess = false)
            }
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

            val isOwn = currentUserId == userId
            val isBlockedByMe = if (!isOwn) {
                (blockRepository.isBlockedByMe(userId) as? Result.Success)?.data ?: false
            } else {
                false
            }

            _state.update {
                it.copy(
                    profile = profile,
                    recentRuns = recentRuns,
                    isLoading = false,
                    isOwnProfile = isOwn,
                    isBlockedByMe = isBlockedByMe,
                )
            }
        }
    }

    private fun blockUser() {
        viewModelScope.launch {
            _state.update { it.copy(showBlockConfirm = false, isBlockLoading = true) }
            val result = blockRepository.blockUser(userId)
            _state.update { it.copy(isBlockLoading = false) }
            if (result is Result.Success) {
                // Blocked user is now invisible — navigate away
                _events.send(UserProfileEvent.NavigateBack)
            }
        }
    }

    private fun unblockUser() {
        viewModelScope.launch {
            _state.update { it.copy(isBlockLoading = true) }
            val result = blockRepository.unblockUser(userId)
            _state.update { it.copy(isBlockLoading = false) }
            if (result is Result.Success) {
                _state.update { it.copy(isBlockedByMe = false) }
            }
        }
    }

    private fun submitReport(reason: String, description: String) {
        val target = _state.value.reportTarget ?: return
        viewModelScope.launch {
            _state.update { it.copy(isReportSubmitting = true) }
            reportRepository.submitReport(
                ReportParams(
                    reportType = "user",
                    reportedUserId = (target as? ReportTarget.User)?.userId,
                    reason = reason,
                    description = description.takeIf { it.isNotBlank() },
                )
            )
            _state.update { it.copy(isReportSubmitting = false, isReportSuccess = true) }
        }
    }
}
