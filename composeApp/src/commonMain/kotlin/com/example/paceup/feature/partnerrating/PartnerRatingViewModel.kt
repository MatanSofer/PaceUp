package com.example.paceup.feature.partnerrating

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.PartnerRatingRepository
import com.example.paceup.shared.runmatching.domain.PartnerTag
import com.example.paceup.shared.runmatching.domain.PartnerToRate
import com.example.paceup.ui.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Per-partner entry shown in the rating list. */
data class PartnerUi(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val paceZone: String?,
)

private fun PartnerToRate.toUi() = PartnerUi(
    userId = userId,
    displayName = displayName,
    avatarUrl = avatarUrl,
    paceZone = paceZone,
)

data class PartnerRatingState(
    val runTitle: String = "",
    val partners: List<PartnerUi> = emptyList(),
    /** Tracks selected tags per partner userId. */
    val selectedTags: Map<String, Set<PartnerTag>> = emptyMap(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    /** Set when the user has already submitted ratings for this run. */
    val alreadyRated: Boolean = false,
    val error: UiText? = null,
)

sealed interface PartnerRatingAction {
    data object OnBackClick : PartnerRatingAction
    data class OnTagToggle(val userId: String, val tag: PartnerTag) : PartnerRatingAction
    data object OnSubmitClick : PartnerRatingAction
    data object OnDismissError : PartnerRatingAction
}

sealed interface PartnerRatingEvent {
    data object NavigateBack : PartnerRatingEvent
    data object SubmitSuccess : PartnerRatingEvent
}

/** Loads partners to rate, manages tag selection, and submits ratings. */
class PartnerRatingViewModel(
    private val partnerRatingRepository: PartnerRatingRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val runId: String = checkNotNull(savedStateHandle["runId"])
    private val runTitle: String = savedStateHandle["runTitle"] ?: ""

    private val _state = MutableStateFlow(PartnerRatingState(runTitle = runTitle))
    val state = _state.asStateFlow()

    private val _events = Channel<PartnerRatingEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadPartners()
    }

    fun onAction(action: PartnerRatingAction) {
        when (action) {
            PartnerRatingAction.OnBackClick -> viewModelScope.launch {
                _events.send(PartnerRatingEvent.NavigateBack)
            }
            is PartnerRatingAction.OnTagToggle -> toggleTag(action.userId, action.tag)
            PartnerRatingAction.OnSubmitClick -> handleSubmit()
            PartnerRatingAction.OnDismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadPartners() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Check if already rated — non-fatal if this call fails; allow rating attempt
            val hasRated = when (val r = partnerRatingRepository.hasRatedRun(runId)) {
                is Result.Success -> r.data
                is Result.Error -> false
            }

            if (hasRated) {
                _state.update { it.copy(isLoading = false, alreadyRated = true) }
                return@launch
            }

            when (val result = partnerRatingRepository.getPartnersToRate(runId)) {
                is Result.Success -> {
                    val partners = result.data.map { it.toUi() }
                    _state.update { it.copy(isLoading = false, partners = partners) }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.DynamicString("Failed to load partners. Please try again."),
                        )
                    }
                }
            }
        }
    }

    private fun toggleTag(userId: String, tag: PartnerTag) {
        _state.update { s ->
            val current = s.selectedTags[userId] ?: emptySet()
            val updated = if (tag in current) current - tag else current + tag
            s.copy(selectedTags = s.selectedTags + (userId to updated))
        }
    }

    private fun handleSubmit() {
        if (_state.value.isSubmitting) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }

            // Build ratings map — all partners included; empty tag set = skipped without tags
            val ratings = _state.value.partners.associate { partner ->
                partner.userId to (_state.value.selectedTags[partner.userId] ?: emptySet())
            }

            when (val result = partnerRatingRepository.submitRatings(runId, ratings)) {
                is Result.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    _events.send(PartnerRatingEvent.SubmitSuccess)
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            error = UiText.DynamicString("Failed to submit ratings. Please try again."),
                        )
                    }
                }
            }
        }
    }
}
