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
)

// Keep HomeState as a typealias so existing usages (nav graph, tooltip) compile unchanged
typealias HomeState = MapDiscoveryState

sealed interface HomeAction {
    data object OnTooltipDismissed : HomeAction
    data class OnPinClick(val run: Run) : HomeAction
    data object OnBottomSheetDismiss : HomeAction
    data class OnModeFilterToggle(val mode: RunMode) : HomeAction
    data object OnVerifiedOnlyToggle : HomeAction
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
                loadRuns()
            }

            HomeAction.OnVerifiedOnlyToggle -> {
                _state.update { it.copy(verifiedOnlyFilter = !it.verifiedOnlyFilter) }
                loadRuns()
            }

            is HomeAction.OnLocationUpdate ->
                // Location is used only for the map camera and the blue dot marker.
                // Runs are always fetched from DefaultMapCenter on init (or on
                // explicit refresh/search) — fetching from the device location would
                // return nothing when the user is far from the seeded data area.
                _state.update { it.copy(userLocation = action.latLng) }

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
                radiusKm = 50.0,
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

    private fun buildFilters(): RunFilters {
        val s = _state.value
        return RunFilters(
            modes = s.activeModeFilters,
            verifiedOnly = if (s.verifiedOnlyFilter) true else null,
        )
    }
}
