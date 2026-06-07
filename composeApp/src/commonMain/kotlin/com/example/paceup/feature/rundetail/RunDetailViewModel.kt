package com.example.paceup.feature.rundetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.feature.report.ReportTarget
import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.ReportParams
import com.example.paceup.shared.runmatching.domain.ReportRepository
import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.RunParticipant
import com.example.paceup.shared.runmatching.domain.RunRepository
import com.example.paceup.shared.runmatching.domain.UserRepository
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tracks whether the current user has a participant row for this run. */
enum class JoinStatus { NONE, REQUESTED, JOINED }

data class RunDetailState(
    val run: Run? = null,
    /** Accepted participants — shown to all viewers. */
    val participants: List<RunParticipant> = emptyList(),
    /** Pending join requests — only populated for the creator (spec §4.2 Smart Invite Logic). */
    val pendingRequests: List<RunParticipant> = emptyList(),
    val isLoading: Boolean = false,
    /** True while a join/cancel/accept/decline API call is in flight. */
    val isJoining: Boolean = false,
    val joinStatus: JoinStatus = JoinStatus.NONE,
    /** True when the current user is the run creator. */
    val isCreator: Boolean = false,
    /**
     * False when the user is a new_runner and the run requires verification (spec §4.4).
     * Button is disabled and shows a "Verified runners only" label.
     */
    val canJoin: Boolean = true,
    val error: UiText? = null,
    val joinError: UiText? = null,
    /** True while the creator's cancel-run confirmation dialog is visible. */
    val showCancelRunDialog: Boolean = false,
    /** True while the cancelRun API call is in flight. */
    val isCancellingRun: Boolean = false,
    val cancelRunError: UiText? = null,
    /** True when the current user's attendance was verified by Strava (status == "attended"). */
    val userAttended: Boolean = false,
    /** Non-null when the report dialog is open. */
    val reportTarget: ReportTarget? = null,
    val isReportSubmitting: Boolean = false,
    val isReportSuccess: Boolean = false,
)

sealed interface RunDetailAction {
    data object OnBackClick : RunDetailAction
    /** Tapped when joinStatus == NONE and canJoin. Calls joinRun or requestToJoin per join_mode. */
    data object OnJoinClick : RunDetailAction
    /** Tapped when joinStatus == JOINED or REQUESTED. Cancels the participant row. */
    data object OnCancelParticipationClick : RunDetailAction
    /** Creator accepts the pending join request for [userId]. */
    data class OnAcceptParticipant(val userId: String) : RunDetailAction
    /** Creator declines the pending join request for [userId]. */
    data class OnDeclineParticipant(val userId: String) : RunDetailAction
    data object OnRetry : RunDetailAction
    data object OnDismissJoinError : RunDetailAction
    /** Accepted participant taps the group chat button. */
    data object OnChatClick : RunDetailAction
    /** Creator taps "Cancel Run" — shows confirmation dialog. */
    data object OnCancelRunClick : RunDetailAction
    /** Creator dismisses the cancel-run confirmation dialog without cancelling. */
    data object OnDismissCancelRunDialog : RunDetailAction
    /** Creator confirms run cancellation with the given [reason]. */
    data class OnConfirmCancelRun(val reason: String) : RunDetailAction
    data object OnDismissCancelRunError : RunDetailAction
    /** User taps a participant row to view their profile. */
    data class OnParticipantClick(val userId: String) : RunDetailAction
    /** Attended user taps "Rate partners". */
    data object OnRatePartnersClick : RunDetailAction
    /** Non-creator taps "Report Run". */
    data object OnReportRunClick : RunDetailAction
    /** User submitted the report form. */
    data class OnSubmitReport(val reason: String, val description: String) : RunDetailAction
    data object OnDismissReport : RunDetailAction
}

sealed interface RunDetailEvent {
    data object NavigateBack : RunDetailEvent
    /** Emitted when the user taps the group chat button (accepted participant only). */
    data class NavigateToChat(val runId: String, val runTitle: String) : RunDetailEvent
    data class NavigateToUserProfile(val userId: String) : RunDetailEvent
    data class NavigateToRatePartners(val runId: String, val runTitle: String) : RunDetailEvent
}

/** Loads run detail, manages join/cancel/accept/decline participant flows, and reporting. */
class RunDetailViewModel(
    private val runRepository: RunRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val runId: String = checkNotNull(savedStateHandle["runId"])
    private var currentUserId: String? = null

    private val _state = MutableStateFlow(RunDetailState())
    val state = _state.asStateFlow()

    private val _events = Channel<RunDetailEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadRunDetail()
    }

    fun onAction(action: RunDetailAction) {
        when (action) {
            RunDetailAction.OnBackClick -> viewModelScope.launch {
                _events.send(RunDetailEvent.NavigateBack)
            }
            RunDetailAction.OnJoinClick -> handleJoinClick()
            RunDetailAction.OnCancelParticipationClick -> handleCancelParticipation()
            is RunDetailAction.OnAcceptParticipant -> handleAcceptParticipant(action.userId)
            is RunDetailAction.OnDeclineParticipant -> handleDeclineParticipant(action.userId)
            RunDetailAction.OnRetry -> loadRunDetail()
            RunDetailAction.OnDismissJoinError -> _state.update { it.copy(joinError = null) }
            RunDetailAction.OnChatClick -> handleChatClick()
            RunDetailAction.OnCancelRunClick -> _state.update { it.copy(showCancelRunDialog = true) }
            RunDetailAction.OnDismissCancelRunDialog -> _state.update { it.copy(showCancelRunDialog = false) }
            is RunDetailAction.OnConfirmCancelRun -> handleCancelRun(action.reason)
            RunDetailAction.OnDismissCancelRunError -> _state.update { it.copy(cancelRunError = null) }
            is RunDetailAction.OnParticipantClick -> viewModelScope.launch {
                _events.send(RunDetailEvent.NavigateToUserProfile(action.userId))
            }
            RunDetailAction.OnRatePartnersClick -> handleRatePartnersClick()
            RunDetailAction.OnReportRunClick -> {
                val run = _state.value.run ?: return
                _state.update {
                    it.copy(reportTarget = ReportTarget.Run(run.id, run.title ?: "Run"))
                }
            }
            is RunDetailAction.OnSubmitReport -> submitReport(action.reason, action.description)
            RunDetailAction.OnDismissReport -> _state.update {
                it.copy(reportTarget = null, isReportSuccess = false)
            }
        }
    }

    private fun loadRunDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Resolve current user — non-fatal if unauthenticated (read-only view still works)
            val userId = when (val r = authRepository.getCurrentUser()) {
                is Result.Success -> r.data?.id
                is Result.Error -> null
            }
            currentUserId = userId

            // Load run
            val runResult = runRepository.getRunById(runId)
            if (runResult is Result.Error) {
                _state.update { it.copy(isLoading = false, error = UiText.DynamicString("Failed to load run")) }
                return@launch
            }
            val run = (runResult as Result.Success).data

            // Load all participant rows (accepted + requested) via one-shot flow
            val allParticipants = runRepository.observeParticipants(runId).firstOrNull() ?: emptyList()

            // Determine creator, join status, and participant split
            val isCreator = userId != null && run.creatorId == userId
            val myStatus = allParticipants.find { it.userId == userId }?.status
            val joinStatus = when (myStatus) {
                "accepted", "attended" -> JoinStatus.JOINED
                "requested" -> JoinStatus.REQUESTED
                else -> JoinStatus.NONE
            }
            val userAttended = myStatus == "attended"
            // Include both accepted (pre-verify) and attended (post-verify) participants
            val accepted = allParticipants.filter { it.status == "accepted" || it.status == "attended" }
            val pending = if (isCreator) allParticipants.filter { it.status == "requested" } else emptyList()

            // Client-side tier gate: new_runner cannot join verified_only runs or non-open runs (spec §4.4)
            val reputationTier = if (userId != null) {
                (userRepository.getReputationTier(userId) as? Result.Success)?.data
            } else null
            val isNewRunner = reputationTier == "new_runner"
            val canJoin = !isCreator &&
                !(isNewRunner && run.verifiedOnly) &&
                !(isNewRunner && run.joinMode != "open")

            _state.update {
                it.copy(
                    run = run,
                    participants = accepted,
                    pendingRequests = pending,
                    isLoading = false,
                    isCreator = isCreator,
                    joinStatus = joinStatus,
                    canJoin = canJoin,
                    userAttended = userAttended,
                )
            }
        }
    }

    private fun handleJoinClick() {
        val run = _state.value.run ?: return
        if (!_state.value.canJoin || _state.value.isJoining) return
        viewModelScope.launch {
            _state.update { it.copy(isJoining = true, joinError = null) }
            val result = when (run.joinMode) {
                "request" -> runRepository.requestToJoin(run.id)
                else -> runRepository.joinRun(run.id)
            }
            when (result) {
                is Result.Success -> {
                    val newStatus = if (run.joinMode == "request") JoinStatus.REQUESTED else JoinStatus.JOINED
                    _state.update { it.copy(isJoining = false, joinStatus = newStatus) }
                }
                is Result.Error -> _state.update {
                    it.copy(isJoining = false, joinError = UiText.DynamicString("Failed to join run. Please try again."))
                }
            }
        }
    }

    private fun handleCancelParticipation() {
        val run = _state.value.run ?: return
        viewModelScope.launch {
            _state.update { it.copy(isJoining = true, joinError = null) }
            val result = runRepository.cancelParticipation(run.id)
            when (result) {
                is Result.Success -> {
                    _state.update { it.copy(isJoining = false, joinStatus = JoinStatus.NONE) }
                    refreshParticipants()
                }
                is Result.Error -> _state.update {
                    it.copy(isJoining = false, joinError = UiText.DynamicString("Failed to cancel. Please try again."))
                }
            }
        }
    }

    private fun handleAcceptParticipant(userId: String) {
        val run = _state.value.run ?: return
        viewModelScope.launch {
            val result = runRepository.acceptParticipant(run.id, userId)
            if (result is Result.Success) refreshParticipants()
        }
    }

    private fun handleDeclineParticipant(userId: String) {
        val run = _state.value.run ?: return
        viewModelScope.launch {
            val result = runRepository.declineParticipant(run.id, userId)
            if (result is Result.Success) refreshParticipants()
        }
    }

    private fun handleChatClick() {
        val run = _state.value.run ?: return
        val title = run.title ?: "${run.mode.name.replaceFirstChar { it.uppercase() }} Run"
        viewModelScope.launch {
            _events.send(RunDetailEvent.NavigateToChat(run.id, title))
        }
    }

    private fun handleCancelRun(reason: String) {
        val run = _state.value.run ?: return
        viewModelScope.launch {
            _state.update { it.copy(showCancelRunDialog = false, isCancellingRun = true, cancelRunError = null) }
            val result = runRepository.cancelRun(run.id, reason)
            when (result) {
                is Result.Success -> {
                    // Reflect the cancelled status locally without a full reload
                    _state.update { s ->
                        s.copy(
                            isCancellingRun = false,
                            run = s.run?.copy(status = com.example.paceup.shared.runmatching.domain.RunStatus.CANCELLED),
                        )
                    }
                }
                is Result.Error -> _state.update {
                    it.copy(
                        isCancellingRun = false,
                        cancelRunError = UiText.DynamicString("Failed to cancel run. Please try again."),
                    )
                }
            }
        }
    }

    private fun handleRatePartnersClick() {
        val run = _state.value.run ?: return
        val title = run.title ?: "${run.mode.name.replaceFirstChar { it.uppercase() }} Run"
        viewModelScope.launch {
            _events.send(RunDetailEvent.NavigateToRatePartners(run.id, title))
        }
    }

    private suspend fun refreshParticipants() {
        val all = runRepository.observeParticipants(runId).firstOrNull() ?: return
        val isCreator = _state.value.isCreator
        _state.update {
            it.copy(
                participants = all.filter { p -> p.status == "accepted" },
                pendingRequests = if (isCreator) all.filter { p -> p.status == "requested" } else emptyList(),
            )
        }
    }

    private fun submitReport(reason: String, description: String) {
        val target = _state.value.reportTarget as? ReportTarget.Run ?: return
        viewModelScope.launch {
            _state.update { it.copy(isReportSubmitting = true) }
            reportRepository.submitReport(
                ReportParams(
                    targetType = "run",
                    targetId = target.runId,
                    reason = reason,
                    description = description.takeIf { it.isNotBlank() },
                )
            )
            _state.update { it.copy(isReportSubmitting = false, isReportSuccess = true) }
        }
    }
}
