package com.example.paceup.feature.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.RunFilters
import com.example.paceup.shared.runmatching.domain.RunMode
import com.example.paceup.shared.runmatching.domain.RunRepository
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class MapDiscoveryState(
    val runs: List<Run> = emptyList(),
    val selectedRun: Run? = null,
    val filters: RunFilters = RunFilters(),
    val activeModeFilters: List<RunMode> = emptyList(),
    val verifiedOnlyFilter: Boolean = false,
    val isLoading: Boolean = false,
    val userLocation: LatLng? = null,
    val error: UiText? = null,
    val showTooltip: Boolean = false,
    val searchQuery: String = "",
)

// Keep HomeState as a typealias so existing usages (nav graph, tooltip) compile unchanged
typealias HomeState = MapDiscoveryState

sealed interface HomeAction {
    data object OnTooltipDismissed : HomeAction
    data class OnPinClick(val run: Run) : HomeAction
    data object OnBottomSheetDismiss : HomeAction
    data class OnModeFilterToggle(val mode: RunMode) : HomeAction
    data object OnVerifiedOnlyToggle : HomeAction
    data class OnSearchQueryChange(val query: String) : HomeAction
    data object OnSearchSubmit : HomeAction
    data class OnLocationUpdate(val latLng: LatLng) : HomeAction
    data object OnRefresh : HomeAction
}

sealed interface HomeEvent {
    data class NavigateToRunDetail(val runId: String) : HomeEvent
    data object NavigateToCreateRun : HomeEvent
}

/** Home screen ViewModel. Loads nearby runs and manages map/filter state. */
class HomeViewModel(
    private val runRepository: RunRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MapDiscoveryState())
    val state = _state.asStateFlow()

    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadRuns()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnTooltipDismissed ->
                _state.update { it.copy(showTooltip = false) }

            is HomeAction.OnPinClick ->
                _state.update { it.copy(selectedRun = action.run) }

            HomeAction.OnBottomSheetDismiss ->
                _state.update { it.copy(selectedRun = null) }

            is HomeAction.OnModeFilterToggle -> {
                val current = _state.value.activeModeFilters.toMutableList()
                if (action.mode in current) current.remove(action.mode) else current.add(action.mode)
                _state.update { it.copy(activeModeFilters = current) }
                applyFilters()
            }

            HomeAction.OnVerifiedOnlyToggle -> {
                _state.update { it.copy(verifiedOnlyFilter = !it.verifiedOnlyFilter) }
                applyFilters()
            }

            is HomeAction.OnSearchQueryChange ->
                _state.update { it.copy(searchQuery = action.query) }

            HomeAction.OnSearchSubmit -> searchRuns()

            is HomeAction.OnLocationUpdate -> {
                _state.update { it.copy(userLocation = action.latLng) }
                loadRuns(center = action.latLng)
            }

            HomeAction.OnRefresh -> loadRuns()
        }
    }

    fun onJoinRunClick(runId: String) {
        viewModelScope.launch {
            _events.send(HomeEvent.NavigateToRunDetail(runId))
        }
    }

    fun onCreateRunClick() {
        viewModelScope.launch {
            _events.send(HomeEvent.NavigateToCreateRun)
        }
    }

    private fun loadRuns(center: LatLng = _state.value.userLocation ?: DefaultMapCenter) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val filters = buildFilters()
            when (val result = runRepository.getRunsNearLocation(
                lat = center.lat,
                lng = center.lng,
                radiusKm = 20.0,
                filters = filters,
            )) {
                is Result.Success -> _state.update {
                    it.copy(runs = result.data, isLoading = false)
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = UiText.DynamicString("Failed to load runs"))
                }
            }
        }
    }

    private fun searchRuns() {
        val query = _state.value.searchQuery.trim()
        if (query.isBlank()) { loadRuns(); return }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = runRepository.searchRuns(query, buildFilters())) {
                is Result.Success -> _state.update {
                    it.copy(runs = result.data, isLoading = false)
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = UiText.DynamicString("Search failed"))
                }
            }
        }
    }

    private fun applyFilters() {
        val s = _state.value
        if (s.searchQuery.isNotBlank()) searchRuns() else loadRuns()
    }

    private fun buildFilters(): RunFilters {
        val s = _state.value
        return RunFilters(
            modes = s.activeModeFilters,
            verifiedOnly = if (s.verifiedOnlyFilter) true else null,
        )
    }
}
