package com.example.paceup.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.BlockRepository
import com.example.paceup.shared.runmatching.domain.UserSummary
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedUsersState(
    val isLoading: Boolean = false,
    val blockedUsers: List<UserSummary> = emptyList(),
    val error: UiText? = null,
    /** ID of the user currently being unblocked (shows inline loading). */
    val unblockingId: String? = null,
)

sealed interface BlockedUsersAction {
    data object OnBackClick : BlockedUsersAction
    data class OnUnblockClick(val userId: String) : BlockedUsersAction
}

sealed interface BlockedUsersEvent {
    data object NavigateBack : BlockedUsersEvent
}

/** Manages the Settings → Privacy → Blocked Users screen (spec §6.1). */
class BlockedUsersViewModel(
    private val blockRepository: BlockRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BlockedUsersState())
    val state = _state.asStateFlow()

    private val _events = Channel<BlockedUsersEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadBlockedUsers()
    }

    fun onAction(action: BlockedUsersAction) {
        when (action) {
            BlockedUsersAction.OnBackClick -> viewModelScope.launch {
                _events.send(BlockedUsersEvent.NavigateBack)
            }
            is BlockedUsersAction.OnUnblockClick -> unblock(action.userId)
        }
    }

    private fun loadBlockedUsers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = blockRepository.getBlockedUsers()) {
                is Result.Success -> _state.update {
                    it.copy(isLoading = false, blockedUsers = result.data)
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = UiText.DynamicString("Failed to load blocked users"))
                }
            }
        }
    }

    private fun unblock(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(unblockingId = userId) }
            val result = blockRepository.unblockUser(userId)
            _state.update { it.copy(unblockingId = null) }
            if (result is Result.Success) {
                _state.update { s ->
                    s.copy(blockedUsers = s.blockedUsers.filterNot { it.id == userId })
                }
            }
        }
    }
}
