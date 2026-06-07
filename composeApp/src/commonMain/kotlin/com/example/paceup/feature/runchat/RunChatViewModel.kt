package com.example.paceup.feature.runchat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.feature.report.ReportTarget
import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.ChatMessage
import com.example.paceup.shared.runmatching.domain.ChatRepository
import com.example.paceup.shared.runmatching.domain.ReportParams
import com.example.paceup.shared.runmatching.domain.ReportRepository
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RunChatState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    /** True when the initial fetch failed but cached/partial data may exist. */
    val isOffline: Boolean = false,
    val currentUserId: String? = null,
    val error: UiText? = null,
    /** Non-null when the report dialog is open for a specific message. */
    val reportTarget: ReportTarget? = null,
    val isReportSubmitting: Boolean = false,
    val isReportSuccess: Boolean = false,
)

sealed interface RunChatAction {
    data object OnBackClick : RunChatAction
    data class OnInputChange(val text: String) : RunChatAction
    data object OnSendClick : RunChatAction
    data object OnRetry : RunChatAction
    data object OnDismissError : RunChatAction
    /** Long-pressed a message from another user — open the report dialog. */
    data class OnLongPressMessage(val message: ChatMessage) : RunChatAction
    /** User submitted the report form. */
    data class OnSubmitReport(val reason: String, val description: String) : RunChatAction
    data object OnDismissReport : RunChatAction
}

sealed interface RunChatEvent {
    data object NavigateBack : RunChatEvent
}

/** Loads run chat history, subscribes to Supabase Realtime for live messages, and handles message reporting. */
class RunChatViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val runId: String = checkNotNull(savedStateHandle["runId"])

    private val _state = MutableStateFlow(RunChatState())
    val state = _state.asStateFlow()

    private val _events = Channel<RunChatEvent>()
    val events = _events.receiveAsFlow()

    init {
        resolveUser()
        loadHistory()
        subscribeRealtime()
    }

    fun onAction(action: RunChatAction) {
        when (action) {
            RunChatAction.OnBackClick -> viewModelScope.launch {
                _events.send(RunChatEvent.NavigateBack)
            }
            is RunChatAction.OnInputChange -> _state.update { it.copy(input = action.text) }
            RunChatAction.OnSendClick -> handleSend()
            RunChatAction.OnRetry -> {
                _state.update { it.copy(error = null, isOffline = false) }
                loadHistory()
            }
            RunChatAction.OnDismissError -> _state.update { it.copy(error = null) }
            is RunChatAction.OnLongPressMessage -> {
                val message = action.message
                if (message.userId != _state.value.currentUserId) {
                    _state.update {
                        it.copy(
                            reportTarget = ReportTarget.Message(
                                senderId = message.userId,
                                senderName = message.senderName,
                                messageContent = message.content,
                            )
                        )
                    }
                }
            }
            is RunChatAction.OnSubmitReport -> submitReport(action.reason, action.description)
            RunChatAction.OnDismissReport -> _state.update {
                it.copy(reportTarget = null, isReportSuccess = false)
            }
        }
    }

    private fun resolveUser() {
        viewModelScope.launch {
            val userId = (authRepository.getCurrentUser() as? Result.Success)?.data?.id
            _state.update { it.copy(currentUserId = userId) }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = chatRepository.getRecentMessages(runId)) {
                is Result.Success -> _state.update {
                    it.copy(messages = result.data, isLoading = false, isOffline = false)
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, isOffline = true)
                }
            }
        }
    }

    private fun subscribeRealtime() {
        viewModelScope.launch {
            chatRepository.observeNewMessages(runId)
                .catch { e ->
                    // Offline — continue with existing cached messages, show banner
                    _state.update { it.copy(isOffline = true) }
                }
                .collect { message ->
                    // Deduplicate: skip if the message id is already in the list
                    if (_state.value.messages.none { it.id == message.id }) {
                        _state.update { it.copy(messages = it.messages + message) }
                    }
                }
        }
    }

    private fun submitReport(reason: String, description: String) {
        val target = _state.value.reportTarget as? ReportTarget.Message ?: return
        viewModelScope.launch {
            _state.update { it.copy(isReportSubmitting = true) }
            reportRepository.submitReport(
                ReportParams(
                    reportType = "message",
                    reportedUserId = target.senderId,
                    reason = reason,
                    description = buildString {
                        append("Message: \"${target.messageContent}\"")
                        if (description.isNotBlank()) append("\n\n$description")
                    },
                )
            )
            _state.update { it.copy(isReportSubmitting = false, isReportSuccess = true) }
        }
    }

    private fun handleSend() {
        val text = _state.value.input.trim()
        if (text.isBlank() || _state.value.isSending) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, input = "") }
            when (chatRepository.sendMessage(runId, text)) {
                is Result.Success -> {
                    _state.update { it.copy(isSending = false) }
                    // Reload history so the sent message appears immediately,
                    // regardless of Realtime connection timing.
                    loadHistory()
                }
                is Result.Error -> _state.update {
                    it.copy(
                        isSending = false,
                        input = text, // restore text so user can retry
                        error = UiText.DynamicString("Failed to send. Please try again."),
                    )
                }
            }
        }
    }
}
