package com.example.paceup.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.RunRepository
import com.example.paceup.shared.runmatching.domain.UserRepository
import com.example.paceup.shared.runmatching.domain.UserSummary
import com.example.paceup.ui.UiText
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchTab { RUNS, PEOPLE }

data class SearchState(
    val query: String = "",
    val activeTab: SearchTab = SearchTab.RUNS,
    val runResults: List<Run> = emptyList(),
    val userResults: List<UserSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
)

sealed interface SearchAction {
    data class OnQueryChange(val query: String) : SearchAction
    data class OnTabChange(val tab: SearchTab) : SearchAction
    data class OnRunClick(val runId: String) : SearchAction
    data class OnUserClick(val userId: String) : SearchAction
    data object OnClearQuery : SearchAction
    data object OnBackClick : SearchAction
}

sealed interface SearchEvent {
    data class NavigateToRunDetail(val runId: String) : SearchEvent
    data class NavigateToUserProfile(val userId: String) : SearchEvent
    data object NavigateBack : SearchEvent
}

/**
 * ViewModel for the full-screen search experience.
 * Run and user results are debounced 300ms after the last keystroke (SPEC.md §10.6).
 * Reusable from rival-request flow and any person-lookup entry point.
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val runRepository: RunRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private val _events = Channel<SearchEvent>()
    val events = _events.receiveAsFlow()

    init {
        // Debounce keystrokes — only fire a search 300ms after the user stops typing.
        viewModelScope.launch {
            _state
                .map { it.query }
                .distinctUntilChanged()
                .debounce(300L)
                .collect { query ->
                    if (query.isNotBlank()) performSearch(query)
                    else clearResults()
                }
        }
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.OnQueryChange ->
                _state.update { it.copy(query = action.query, error = null) }

            is SearchAction.OnTabChange -> {
                _state.update { it.copy(activeTab = action.tab) }
                val q = _state.value.query.trim()
                if (q.isNotBlank()) viewModelScope.launch { performSearch(q) }
            }

            is SearchAction.OnRunClick -> viewModelScope.launch {
                _events.send(SearchEvent.NavigateToRunDetail(action.runId))
            }

            is SearchAction.OnUserClick -> viewModelScope.launch {
                _events.send(SearchEvent.NavigateToUserProfile(action.userId))
            }

            SearchAction.OnClearQuery ->
                _state.update { it.copy(query = "", runResults = emptyList(), userResults = emptyList(), error = null) }

            SearchAction.OnBackClick -> viewModelScope.launch {
                _events.send(SearchEvent.NavigateBack)
            }
        }
    }

    private suspend fun performSearch(query: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        when (_state.value.activeTab) {
            SearchTab.RUNS -> {
                when (val result = runRepository.searchRuns(query)) {
                    is Result.Success ->
                        _state.update { it.copy(runResults = result.data, isLoading = false) }
                    is Result.Error ->
                        _state.update { it.copy(isLoading = false, error = UiText.DynamicString("Search failed")) }
                }
            }
            SearchTab.PEOPLE -> {
                when (val result = userRepository.searchUsers(query)) {
                    is Result.Success ->
                        _state.update { it.copy(userResults = result.data, isLoading = false) }
                    is Result.Error ->
                        _state.update { it.copy(isLoading = false, error = UiText.DynamicString("Search failed")) }
                }
            }
        }
    }

    private fun clearResults() {
        _state.update { it.copy(runResults = emptyList(), userResults = emptyList(), isLoading = false) }
    }
}
