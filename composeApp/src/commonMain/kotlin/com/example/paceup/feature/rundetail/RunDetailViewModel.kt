package com.example.paceup.feature.rundetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.RunParticipant
import com.example.paceup.shared.runmatching.domain.RunRepository
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RunDetailState(
    val run: Run? = null,
    val participants: List<RunParticipant> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
)

sealed interface RunDetailAction {
    data object OnBackClick : RunDetailAction
    data object OnJoinClick : RunDetailAction
    data object OnRetry : RunDetailAction
}

sealed interface RunDetailEvent {
    data object NavigateBack : RunDetailEvent
    /** Join CTA tapped — actual join logic wired in Task 5.2. */
    data class JoinRun(val runId: String) : RunDetailEvent
}

/** ViewModel for the run detail screen. Loads run + participant data. */
class RunDetailViewModel(
    private val runRepository: RunRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val runId: String = checkNotNull(savedStateHandle["runId"])

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
            RunDetailAction.OnJoinClick -> viewModelScope.launch {
                _state.value.run?.let { _events.send(RunDetailEvent.JoinRun(it.id)) }
            }
            RunDetailAction.OnRetry -> loadRunDetail()
        }
    }

    private fun loadRunDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val runResult = runRepository.getRunById(runId)
            if (runResult is Result.Error) {
                _state.update { it.copy(isLoading = false, error = UiText.DynamicString("Failed to load run")) }
                return@launch
            }
            val run = (runResult as Result.Success).data

            val participants = when (val pr = runRepository.getRunParticipants(runId)) {
                is Result.Success -> pr.data
                is Result.Error -> emptyList()
            }

            _state.update { it.copy(run = run, participants = participants, isLoading = false) }
        }
    }
}
