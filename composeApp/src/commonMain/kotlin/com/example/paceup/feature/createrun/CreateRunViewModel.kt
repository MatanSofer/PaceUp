package com.example.paceup.feature.createrun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.CreateRunParams
import com.example.paceup.shared.runmatching.domain.RunMode
import com.example.paceup.shared.runmatching.domain.RunRepository
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** MVP run modes available at launch. Post-MVP modes deferred per plan Task 4.2. */
val MVP_RUN_MODES = listOf(RunMode.EASY, RunMode.TEMPO, RunMode.RECOVERY)

data class CreateRunState(
    val step: Int = 1,

    // Step 1 — Mode
    val selectedMode: RunMode? = null,

    // Step 2 — Date & time (simple text inputs — map picker deferred to post-MVP)
    val scheduledDate: String = "",
    val scheduledTime: String = "",

    // Step 3 — Location
    val meetingAddress: String = "",
    val city: String = "",
    val meetingLatText: String = "",
    val meetingLngText: String = "",
    /** True once the first GPS fix has been applied — prevents overwriting user edits. */
    val gpsLoaded: Boolean = false,

    // Step 4 — Run details
    val title: String = "",
    val description: String = "",
    val distanceKmText: String = "",
    val durationMinText: String = "",
    val paceMinSec: Int = 270,
    val paceMaxSec: Int = 390,

    // Step 5 — Filters
    val maxParticipantsText: String = "",
    val ageMinText: String = "",
    val ageMaxText: String = "",
    val genderFilter: String = "any",
    val verifiedOnly: Boolean = false,

    // Step 6 — Join mode
    val joinMode: String = "open",

    // Step 7 — Review
    val isLoading: Boolean = false,
    val error: UiText? = null,
) {
    val totalSteps: Int get() = 7
}

sealed interface CreateRunAction {
    // Step 1
    data class OnModeSelected(val mode: RunMode) : CreateRunAction

    // Step 2
    data class OnDateChanged(val date: String) : CreateRunAction
    data class OnTimeChanged(val time: String) : CreateRunAction

    // Step 3
    data class OnAddressChanged(val address: String) : CreateRunAction
    data class OnCityChanged(val city: String) : CreateRunAction
    data class OnLatChanged(val lat: String) : CreateRunAction
    data class OnLngChanged(val lng: String) : CreateRunAction

    // Step 4
    data class OnTitleChanged(val title: String) : CreateRunAction
    data class OnDescriptionChanged(val desc: String) : CreateRunAction
    data class OnDistanceChanged(val km: String) : CreateRunAction
    data class OnDurationChanged(val min: String) : CreateRunAction
    data class OnPaceMinChanged(val sec: Int) : CreateRunAction
    data class OnPaceMaxChanged(val sec: Int) : CreateRunAction

    // Step 5
    data class OnMaxParticipantsChanged(val value: String) : CreateRunAction
    data class OnAgeMinChanged(val value: String) : CreateRunAction
    data class OnAgeMaxChanged(val value: String) : CreateRunAction
    data class OnGenderFilterChanged(val value: String) : CreateRunAction
    data object OnVerifiedOnlyToggled : CreateRunAction

    // Step 3 — GPS auto-fill (fires once when LocationEffect delivers device coords)
    data class OnGpsLocationReceived(val lat: Double, val lng: Double) : CreateRunAction

    // Step 6
    data class OnJoinModeChanged(val mode: String) : CreateRunAction

    // Navigation
    data object OnNextStep : CreateRunAction
    data object OnPreviousStep : CreateRunAction
    data object OnBackClick : CreateRunAction
    data object OnCreateRun : CreateRunAction
    data object OnDismissError : CreateRunAction
}

sealed interface CreateRunEvent {
    data object NavigateBack : CreateRunEvent
    data class NavigateToRunDetail(val runId: String) : CreateRunEvent
}

/** ViewModel for the multi-step create run flow (SPEC.md §4.2). */
class CreateRunViewModel(
    private val runRepository: RunRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateRunState())
    val state = _state.asStateFlow()

    private val _events = Channel<CreateRunEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: CreateRunAction) {
        when (action) {
            is CreateRunAction.OnModeSelected -> _state.update { it.copy(selectedMode = action.mode) }
            is CreateRunAction.OnDateChanged -> _state.update { it.copy(scheduledDate = action.date) }
            is CreateRunAction.OnTimeChanged -> _state.update { it.copy(scheduledTime = action.time) }
            is CreateRunAction.OnAddressChanged -> _state.update { it.copy(meetingAddress = action.address) }
            is CreateRunAction.OnCityChanged -> _state.update { it.copy(city = action.city) }
            is CreateRunAction.OnLatChanged -> _state.update { it.copy(meetingLatText = action.lat) }
            is CreateRunAction.OnLngChanged -> _state.update { it.copy(meetingLngText = action.lng) }
            is CreateRunAction.OnTitleChanged -> _state.update { it.copy(title = action.title) }
            is CreateRunAction.OnDescriptionChanged -> _state.update { it.copy(description = action.desc) }
            is CreateRunAction.OnDistanceChanged -> _state.update { it.copy(distanceKmText = action.km) }
            is CreateRunAction.OnDurationChanged -> _state.update { it.copy(durationMinText = action.min) }
            is CreateRunAction.OnPaceMinChanged -> _state.update { it.copy(paceMinSec = action.sec) }
            is CreateRunAction.OnPaceMaxChanged -> _state.update { it.copy(paceMaxSec = action.sec) }
            is CreateRunAction.OnMaxParticipantsChanged -> _state.update { it.copy(maxParticipantsText = action.value) }
            is CreateRunAction.OnAgeMinChanged -> _state.update { it.copy(ageMinText = action.value) }
            is CreateRunAction.OnAgeMaxChanged -> _state.update { it.copy(ageMaxText = action.value) }
            is CreateRunAction.OnGenderFilterChanged -> _state.update { it.copy(genderFilter = action.value) }
            CreateRunAction.OnVerifiedOnlyToggled -> _state.update { it.copy(verifiedOnly = !it.verifiedOnly) }
            is CreateRunAction.OnGpsLocationReceived -> {
                if (!_state.value.gpsLoaded) {
                    _state.update {
                        it.copy(
                            meetingLatText = "%.6f".format(action.lat),
                            meetingLngText = "%.6f".format(action.lng),
                            gpsLoaded = true,
                        )
                    }
                }
            }
            is CreateRunAction.OnJoinModeChanged -> _state.update { it.copy(joinMode = action.mode) }
            CreateRunAction.OnNextStep -> advanceStep()
            CreateRunAction.OnPreviousStep -> goBackStep()
            CreateRunAction.OnBackClick -> viewModelScope.launch {
                _events.send(CreateRunEvent.NavigateBack)
            }
            CreateRunAction.OnCreateRun -> createRun()
            CreateRunAction.OnDismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun advanceStep() {
        val s = _state.value
        val validationError = validateStep(s)
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }
        if (s.step < s.totalSteps) {
            _state.update { it.copy(step = s.step + 1, error = null) }
        }
    }

    private fun goBackStep() {
        val s = _state.value
        if (s.step > 1) {
            _state.update { it.copy(step = s.step - 1, error = null) }
        } else {
            viewModelScope.launch { _events.send(CreateRunEvent.NavigateBack) }
        }
    }

    private fun validateStep(s: CreateRunState): UiText? = when (s.step) {
        1 -> if (s.selectedMode == null) UiText.DynamicString("Select a run mode to continue") else null

        2 -> validateDateTime(s.scheduledDate, s.scheduledTime)

        3 -> validateLocation(s)

        4 -> validateDetails(s)

        5 -> validateFilters(s)

        else -> null
    }

    private fun validateDateTime(date: String, time: String): UiText? {
        if (date.isBlank()) return UiText.DynamicString("Enter the run date (YYYY-MM-DD)")
        val dateParts = date.split("-")
        if (dateParts.size != 3 || dateParts.any { it.toIntOrNull() == null }) {
            return UiText.DynamicString("Date must be in YYYY-MM-DD format (e.g. 2026-06-15)")
        }
        val (year, month, day) = dateParts.map { it.toInt() }
        if (year < 2026) return UiText.DynamicString("Year must be 2026 or later")
        if (month !in 1..12) return UiText.DynamicString("Month must be between 01 and 12")
        if (day !in 1..31) return UiText.DynamicString("Day must be between 01 and 31")

        if (time.isBlank()) return UiText.DynamicString("Enter the run time (HH:MM)")
        val timeParts = time.split(":")
        if (timeParts.size != 2 || timeParts.any { it.toIntOrNull() == null }) {
            return UiText.DynamicString("Time must be in HH:MM format (e.g. 06:30)")
        }
        val (hour, minute) = timeParts.map { it.toInt() }
        if (hour !in 0..23) return UiText.DynamicString("Hour must be between 00 and 23")
        if (minute !in 0..59) return UiText.DynamicString("Minute must be between 00 and 59")

        return null
    }

    private fun validateLocation(s: CreateRunState): UiText? {
        if (s.meetingAddress.isBlank()) return UiText.DynamicString("Enter the meeting address")
        if (s.city.isBlank()) return UiText.DynamicString("Enter the city")

        val lat = s.meetingLatText.toDoubleOrNull()
            ?: return UiText.DynamicString("Latitude must be a number (e.g. 32.08)")
        if (lat !in -90.0..90.0) return UiText.DynamicString("Latitude must be between -90 and 90")

        val lng = s.meetingLngText.toDoubleOrNull()
            ?: return UiText.DynamicString("Longitude must be a number (e.g. 34.78)")
        if (lng !in -180.0..180.0) return UiText.DynamicString("Longitude must be between -180 and 180")

        return null
    }

    private fun validateDetails(s: CreateRunState): UiText? {
        if (s.distanceKmText.isBlank() && s.durationMinText.isBlank()) {
            return UiText.DynamicString("Enter a distance or duration")
        }
        if (s.distanceKmText.isNotBlank()) {
            val km = s.distanceKmText.toFloatOrNull()
                ?: return UiText.DynamicString("Distance must be a number (e.g. 10)")
            if (km <= 0f) return UiText.DynamicString("Distance must be greater than 0")
            if (km > 200f) return UiText.DynamicString("Distance must be 200 km or less")
        }
        if (s.durationMinText.isNotBlank()) {
            val min = s.durationMinText.toIntOrNull()
                ?: return UiText.DynamicString("Duration must be a whole number of minutes (e.g. 60)")
            if (min <= 0) return UiText.DynamicString("Duration must be greater than 0")
            if (min > 720) return UiText.DynamicString("Duration must be 720 minutes (12 hours) or less")
        }
        if (s.paceMinSec >= s.paceMaxSec) {
            return UiText.DynamicString("Min pace must be faster than max pace — move the top slider left")
        }
        return null
    }

    private fun validateFilters(s: CreateRunState): UiText? {
        if (s.maxParticipantsText.isNotBlank()) {
            val max = s.maxParticipantsText.toIntOrNull()
                ?: return UiText.DynamicString("Max participants must be a whole number (e.g. 10)")
            if (max < 2) return UiText.DynamicString("Max participants must be at least 2")
            if (max > 500) return UiText.DynamicString("Max participants must be 500 or less")
        }
        val ageMin = s.ageMinText.toIntOrNull()
        val ageMax = s.ageMaxText.toIntOrNull()
        if (s.ageMinText.isNotBlank() && ageMin == null) {
            return UiText.DynamicString("Min age must be a whole number (e.g. 18)")
        }
        if (s.ageMaxText.isNotBlank() && ageMax == null) {
            return UiText.DynamicString("Max age must be a whole number (e.g. 60)")
        }
        if (ageMin != null && ageMin !in 1..120) {
            return UiText.DynamicString("Min age must be between 1 and 120")
        }
        if (ageMax != null && ageMax !in 1..120) {
            return UiText.DynamicString("Max age must be between 1 and 120")
        }
        if (ageMin != null && ageMax != null && ageMin >= ageMax) {
            return UiText.DynamicString("Min age must be less than max age")
        }
        return null
    }

    private fun createRun() {
        viewModelScope.launch {
            val s = _state.value
            _state.update { it.copy(isLoading = true, error = null) }

            val userResult = authRepository.getCurrentUser()
            val userId = when (userResult) {
                is Result.Success -> userResult.data?.id
                is Result.Error -> null
            }
            if (userId == null) {
                _state.update { it.copy(isLoading = false, error = UiText.DynamicString("Not signed in")) }
                return@launch
            }

            val scheduledAt = buildIso8601(s.scheduledDate, s.scheduledTime)
            val params = CreateRunParams(
                creatorId = userId,
                title = s.title.takeIf { it.isNotBlank() },
                description = s.description.takeIf { it.isNotBlank() },
                mode = s.selectedMode ?: RunMode.EASY,
                scheduledAt = scheduledAt,
                meetingLat = s.meetingLatText.toDouble(),
                meetingLng = s.meetingLngText.toDouble(),
                meetingAddress = s.meetingAddress,
                city = s.city,
                distanceKm = s.distanceKmText.toFloatOrNull(),
                durationMin = s.durationMinText.toIntOrNull(),
                paceMinSec = s.paceMinSec,
                paceMaxSec = s.paceMaxSec,
                maxParticipants = s.maxParticipantsText.toIntOrNull(),
                ageMin = s.ageMinText.toIntOrNull(),
                ageMax = s.ageMaxText.toIntOrNull(),
                genderFilter = s.genderFilter,
                verifiedOnly = s.verifiedOnly,
                joinMode = s.joinMode,
            )

            when (val result = runRepository.createRun(params)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(CreateRunEvent.NavigateToRunDetail(result.data.id))
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(isLoading = false, error = UiText.DynamicString("Failed to create run. Please try again."))
                    }
                }
            }
        }
    }

    /** Converts "2026-06-01" + "06:30" → "2026-06-01T06:30:00Z". */
    private fun buildIso8601(date: String, time: String): String {
        val normalizedTime = if (time.length == 5) "${time}:00" else time
        return "${date}T${normalizedTime}Z"
    }
}
