package com.example.paceup.feature.rivaldashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.rivalengine.domain.Rival
import com.example.paceup.shared.rivalengine.domain.RivalRepository
import com.example.paceup.shared.rivalengine.domain.RivalWeeklySnapshot
import com.example.paceup.shared.runmatching.domain.UserRepository
import com.example.paceup.shared.runmatching.domain.UserSummary
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A rival enriched with the opponent's profile data and this week's snapshot. */
data class RivalWithDetails(
    val rival: Rival,
    val snapshot: RivalWeeklySnapshot?,
    val opponentSummary: UserSummary?,
    /** True when the current user is user_b_id (received the request). */
    val isIncoming: Boolean,
)

data class RivalDashboardState(
    val isLoading: Boolean = true,
    val currentUserId: String = "",
    val currentUserDisplayName: String = "",
    /** Active rival connections (status = "active"). */
    val activeRivals: List<RivalWithDetails> = emptyList(),
    /** Incoming pending requests (current user is user_b_id). */
    val incomingRequests: List<RivalWithDetails> = emptyList(),
    /** Outgoing pending requests (current user is user_a_id). */
    val outgoingRequests: List<RivalWithDetails> = emptyList(),
    val error: UiText? = null,
    /** True while the "add rival" bottom sheet is open. */
    val showAddSheet: Boolean = false,
    val addSearchQuery: String = "",
    val addSearchResults: List<UserSummary> = emptyList(),
    val isSearching: Boolean = false,
    val addError: UiText? = null,
)

sealed interface RivalDashboardAction {
    data object OnRefresh : RivalDashboardAction
    data object OnBackClick : RivalDashboardAction
    data object OnAddRivalClick : RivalDashboardAction
    data object OnDismissAddSheet : RivalDashboardAction
    data class OnAddSearchQueryChange(val query: String) : RivalDashboardAction
    data class OnSendRivalRequest(val targetUserId: String) : RivalDashboardAction
    data class OnAcceptRival(val rivalId: String) : RivalDashboardAction
    data class OnDeclineRival(val rivalId: String) : RivalDashboardAction
}

sealed interface RivalDashboardEvent {
    data object NavigateBack : RivalDashboardEvent
}

/**
 * ViewModel for the rival dashboard screen (spec §4.5).
 * Loads rivals, enriches them with opponent profiles and weekly snapshots,
 * and observes real-time incoming rival requests.
 */
class RivalDashboardViewModel(
    private val rivalRepository: RivalRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RivalDashboardState())
    val state = _state.asStateFlow()

    private val _events = Channel<RivalDashboardEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadRivals()
        observeIncomingRequests()
    }

    fun onAction(action: RivalDashboardAction) {
        when (action) {
            is RivalDashboardAction.OnRefresh            -> loadRivals()
            is RivalDashboardAction.OnBackClick          -> viewModelScope.launch { _events.send(RivalDashboardEvent.NavigateBack) }
            is RivalDashboardAction.OnAddRivalClick      -> _state.update { it.copy(showAddSheet = true, addSearchQuery = "", addSearchResults = emptyList(), addError = null) }
            is RivalDashboardAction.OnDismissAddSheet    -> _state.update { it.copy(showAddSheet = false) }
            is RivalDashboardAction.OnAddSearchQueryChange -> onSearchQueryChange(action.query)
            is RivalDashboardAction.OnSendRivalRequest   -> sendRivalRequest(action.targetUserId)
            is RivalDashboardAction.OnAcceptRival        -> acceptRival(action.rivalId)
            is RivalDashboardAction.OnDeclineRival       -> declineRival(action.rivalId)
        }
    }

    private fun loadRivals() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentUser = when (val r = authRepository.getCurrentUser()) {
                is Result.Success -> r.data
                is Result.Error   -> null
            }
            val userId = currentUser?.id ?: run {
                _state.update { it.copy(isLoading = false, error = UiText.DynamicString("Not signed in")) }
                return@launch
            }

            when (val rivalsResult = rivalRepository.getRivals()) {
                is Result.Error   -> _state.update { it.copy(isLoading = false, error = UiText.DynamicString("Failed to load rivals")) }
                is Result.Success -> {
                    val rivals = rivalsResult.data
                    val enriched = rivals.map { rival ->
                        val opponentId = if (rival.userAId == userId) rival.userBId else rival.userAId
                        val opponent = (userRepository.getUserSummary(opponentId) as? Result.Success)?.data
                        val snapshot = (rivalRepository.getLatestSnapshot(rival.id) as? Result.Success)?.data
                        RivalWithDetails(
                            rival = rival,
                            snapshot = snapshot,
                            opponentSummary = opponent,
                            isIncoming = rival.userBId == userId,
                        )
                    }

                    val active   = enriched.filter { it.rival.status == "active" }
                    val incoming = enriched.filter { it.rival.status == "pending" && it.isIncoming }
                    val outgoing = enriched.filter { it.rival.status == "pending" && !it.isIncoming }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentUserId = userId,
                            activeRivals = active,
                            incomingRequests = incoming,
                            outgoingRequests = outgoing,
                        )
                    }
                }
            }
        }
    }

    private fun observeIncomingRequests() {
        viewModelScope.launch {
            rivalRepository.observeRivalRequest()
                .catch { /* silently ignore realtime errors */ }
                .collect { newRival ->
                    val currentUserId = _state.value.currentUserId
                    val opponentId = if (newRival.userAId == currentUserId) newRival.userBId else newRival.userAId
                    val opponent = (userRepository.getUserSummary(opponentId) as? Result.Success)?.data
                    val details = RivalWithDetails(
                        rival = newRival,
                        snapshot = null,
                        opponentSummary = opponent,
                        isIncoming = true,
                    )
                    _state.update { s ->
                        s.copy(incomingRequests = (s.incomingRequests + details).distinctBy { it.rival.id })
                    }
                }
        }
    }

    private fun onSearchQueryChange(query: String) {
        _state.update { it.copy(addSearchQuery = query) }
        if (query.length < 2) {
            _state.update { it.copy(addSearchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            val result = userRepository.searchUsers(query)
            _state.update {
                it.copy(
                    isSearching = false,
                    addSearchResults = (result as? Result.Success)?.data ?: emptyList(),
                )
            }
        }
    }

    private fun sendRivalRequest(targetUserId: String) {
        viewModelScope.launch {
            val result = rivalRepository.sendRivalRequest(targetUserId)
            if (result is Result.Error) {
                _state.update { it.copy(addError = UiText.DynamicString("Failed to send rival request")) }
            } else {
                _state.update { it.copy(showAddSheet = false, addError = null) }
                loadRivals()
            }
        }
    }

    private fun acceptRival(rivalId: String) {
        viewModelScope.launch {
            rivalRepository.acceptRivalRequest(rivalId)
            loadRivals()
        }
    }

    private fun declineRival(rivalId: String) {
        viewModelScope.launch {
            rivalRepository.declineRivalRequest(rivalId)
            loadRivals()
        }
    }
}
