package com.example.paceup.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.pacezone.PaceZone
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
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/** How runs are ordered in the list. */
enum class RunSortOrder { SOONEST, CLOSEST }

/** Date range quick presets for the list filter. */
enum class DatePreset { ANY, TODAY_ONWARDS, NEXT_7_DAYS }

/**
 * Presentation-layer filter state for the list discovery screen.
 * Richer than [RunFilters] — includes UI-only state like [proximityKm] and [datePreset].
 */
data class RunListFilters(
    val paceZones: List<PaceZone> = emptyList(),
    val modes: List<RunMode> = emptyList(),
    val proximityKm: Double? = null,
    val runMinDistanceKm: Float? = null,
    val datePreset: DatePreset = DatePreset.ANY,
    val verifiedOnly: Boolean = false,
    val openJoinOnly: Boolean = false,
    val recurringOnly: Boolean = false,
) {
    /** Number of non-default filters active — used for the filter badge count. */
    val activeCount: Int get() = listOf(
        paceZones.isNotEmpty(),
        modes.isNotEmpty(),
        proximityKm != null,
        runMinDistanceKm != null,
        datePreset != DatePreset.ANY,
        verifiedOnly,
        openJoinOnly,
        recurringOnly,
    ).count { it }
}

data class RunListState(
    val runs: List<Run> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val sortOrder: RunSortOrder = RunSortOrder.SOONEST,
    val filters: RunListFilters = RunListFilters(),
    val showFilterSheet: Boolean = false,
    val userLocation: LatLng? = null,
)

sealed interface RunListAction {
    data class OnRunClick(val runId: String) : RunListAction
    data object OnRefresh : RunListAction
    data object OnToggleFilterSheet : RunListAction
    data class OnSortOrderChange(val sortOrder: RunSortOrder) : RunListAction
    data class OnPaceZoneToggle(val zone: PaceZone) : RunListAction
    data class OnModeToggle(val mode: RunMode) : RunListAction
    data class OnProximityChange(val km: Double?) : RunListAction
    data class OnRunMinDistanceChange(val km: Float?) : RunListAction
    data class OnDatePresetChange(val preset: DatePreset) : RunListAction
    data object OnVerifiedOnlyToggle : RunListAction
    data object OnOpenJoinOnlyToggle : RunListAction
    data object OnRecurringOnlyToggle : RunListAction
    data object OnClearFilters : RunListAction
    data class OnLocationUpdate(val latLng: LatLng) : RunListAction
}

sealed interface RunListEvent {
    data class NavigateToRunDetail(val runId: String) : RunListEvent
}

/** ViewModel for the list discovery screen. Loads runs and manages filter/sort state. */
class RunListViewModel(
    private val runRepository: RunRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RunListState())
    val state = _state.asStateFlow()

    private val _events = Channel<RunListEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadRuns()
    }

    fun onAction(action: RunListAction) {
        when (action) {
            is RunListAction.OnRunClick -> viewModelScope.launch {
                _events.send(RunListEvent.NavigateToRunDetail(action.runId))
            }

            RunListAction.OnRefresh -> loadRuns()

            RunListAction.OnToggleFilterSheet ->
                _state.update { it.copy(showFilterSheet = !it.showFilterSheet) }

            is RunListAction.OnSortOrderChange -> {
                _state.update { it.copy(sortOrder = action.sortOrder) }
                applySort()
            }

            is RunListAction.OnPaceZoneToggle -> updateFilters {
                val zones = it.paceZones.toMutableList()
                if (action.zone in zones) zones.remove(action.zone) else zones.add(action.zone)
                it.copy(paceZones = zones)
            }

            is RunListAction.OnModeToggle -> updateFilters {
                val modes = it.modes.toMutableList()
                if (action.mode in modes) modes.remove(action.mode) else modes.add(action.mode)
                it.copy(modes = modes)
            }

            is RunListAction.OnProximityChange ->
                updateFilters { it.copy(proximityKm = action.km) }

            is RunListAction.OnRunMinDistanceChange ->
                updateFilters { it.copy(runMinDistanceKm = action.km) }

            is RunListAction.OnDatePresetChange ->
                updateFilters { it.copy(datePreset = action.preset) }

            RunListAction.OnVerifiedOnlyToggle ->
                updateFilters { it.copy(verifiedOnly = !it.verifiedOnly) }

            RunListAction.OnOpenJoinOnlyToggle ->
                updateFilters { it.copy(openJoinOnly = !it.openJoinOnly) }

            RunListAction.OnRecurringOnlyToggle ->
                updateFilters { it.copy(recurringOnly = !it.recurringOnly) }

            RunListAction.OnClearFilters -> {
                _state.update { it.copy(filters = RunListFilters()) }
                loadRuns()
            }

            is RunListAction.OnLocationUpdate -> {
                _state.update { it.copy(userLocation = action.latLng) }
                if (_state.value.sortOrder == RunSortOrder.CLOSEST) applySort()
            }
        }
    }

    private fun updateFilters(transform: (RunListFilters) -> RunListFilters) {
        _state.update { it.copy(filters = transform(it.filters)) }
        loadRuns()
    }

    private fun loadRuns() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val center = _state.value.userLocation ?: DefaultMapCenter
            val proximity = _state.value.filters.proximityKm ?: 50.0
            val runFilters = _state.value.filters.toRunFilters()

            when (val result = runRepository.getRunsNearLocation(
                lat = center.lat,
                lng = center.lng,
                radiusKm = proximity,
                filters = runFilters,
            )) {
                is Result.Success -> {
                    val sorted = sortRuns(result.data)
                    _state.update { it.copy(runs = sorted, isLoading = false) }
                }
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = UiText.DynamicString("Failed to load runs"))
                }
            }
        }
    }

    private fun applySort() {
        val sorted = sortRuns(_state.value.runs)
        _state.update { it.copy(runs = sorted) }
    }

    private fun sortRuns(runs: List<Run>): List<Run> {
        val location = _state.value.userLocation
        return when (_state.value.sortOrder) {
            RunSortOrder.SOONEST -> runs.sortedBy { it.scheduledAt }
            RunSortOrder.CLOSEST -> {
                if (location == null) runs.sortedBy { it.scheduledAt }
                else runs.sortedBy { run ->
                    haversineKm(location.lat, location.lng, run.meetingLat, run.meetingLng)
                }
            }
        }
    }

    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) *
            kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2).let { it * it }
        return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }
}

/** Converts presentation-layer filters to the shared [RunFilters] for the repository. */
private fun RunListFilters.toRunFilters(): RunFilters {
    val tz = TimeZone.currentSystemDefault()
    val today = Clock.System.todayIn(tz)
    val (afterDate, beforeDate) = when (datePreset) {
        DatePreset.ANY -> null to null
        DatePreset.TODAY_ONWARDS -> "${today}T00:00:00" to null
        DatePreset.NEXT_7_DAYS -> "${today}T00:00:00" to "${today.plus(7, DateTimeUnit.DAY)}T23:59:59"
    }

    // Convert selected zones to a pace sec range covering all selected zones.
    val paceMin = if (paceZones.isEmpty()) null
        else if (paceZones.any { it.minSecPerKm == null }) null
        else paceZones.mapNotNull { it.minSecPerKm }.minOrNull()
    val paceMax = if (paceZones.isEmpty()) null
        else if (paceZones.any { it.maxSecPerKm == null }) null
        else paceZones.mapNotNull { it.maxSecPerKm }.maxOrNull()

    return RunFilters(
        paceMinSec = paceMin,
        paceMaxSec = paceMax,
        modes = modes,
        verifiedOnly = if (verifiedOnly) true else null,
        minDistanceKm = runMinDistanceKm,
        afterDate = afterDate,
        beforeDate = beforeDate,
        openJoinOnly = if (openJoinOnly) true else null,
        recurringOnly = if (recurringOnly) true else null,
    )
}
