package com.example.paceup.feature.partnerrating

import androidx.lifecycle.SavedStateHandle
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.PartnerTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PartnerRatingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakePartnerRatingRepository
    private lateinit var viewModel: PartnerRatingViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakePartnerRatingRepository()
        viewModel = makeViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(runTitle: String = "Morning Run") = PartnerRatingViewModel(
        partnerRatingRepository = fakeRepo,
        savedStateHandle = SavedStateHandle(mapOf("runId" to "run-1", "runTitle" to runTitle)),
    )

    // ── Initial load ──────────────────────────────────────────────────────────

    @Test
    fun init_loadsPartners_whenNotYetRated() = runTest {
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.alreadyRated)
        assertEquals(2, state.partners.size)
        assertEquals("Alice", state.partners[0].displayName)
        assertNull(state.error)
    }

    @Test
    fun init_setsAlreadyRated_whenHasRatedRun() = runTest {
        fakeRepo.hasRatedResult = Result.Success(true)
        viewModel = makeViewModel()
        val state = viewModel.state.value
        assertTrue(state.alreadyRated)
        assertEquals(0, state.partners.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun init_showsError_whenGetPartnersFails() = runTest {
        fakeRepo.hasRatedResult = Result.Success(false)
        fakeRepo.getPartnersResult = Result.Error(RunError.NETWORK_ERROR)
        viewModel = makeViewModel()
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun init_setsRunTitle_fromSavedState() = runTest {
        assertEquals("Morning Run", viewModel.state.value.runTitle)
    }

    // ── Tag toggling ──────────────────────────────────────────────────────────

    @Test
    fun tagToggle_selectsTag() = runTest {
        viewModel.onAction(PartnerRatingAction.OnTagToggle("user-2", PartnerTag.KEPT_PACE))
        val tags = viewModel.state.value.selectedTags["user-2"]
        assertNotNull(tags)
        assertTrue(PartnerTag.KEPT_PACE in tags)
    }

    @Test
    fun tagToggle_deselectsAlreadySelectedTag() = runTest {
        viewModel.onAction(PartnerRatingAction.OnTagToggle("user-2", PartnerTag.KEPT_PACE))
        viewModel.onAction(PartnerRatingAction.OnTagToggle("user-2", PartnerTag.KEPT_PACE))
        val tags = viewModel.state.value.selectedTags["user-2"] ?: emptySet()
        assertFalse(PartnerTag.KEPT_PACE in tags)
    }

    @Test
    fun tagToggle_multipleTagsForSamePartner() = runTest {
        viewModel.onAction(PartnerRatingAction.OnTagToggle("user-2", PartnerTag.KEPT_PACE))
        viewModel.onAction(PartnerRatingAction.OnTagToggle("user-2", PartnerTag.GREAT_ENERGY))
        val tags = viewModel.state.value.selectedTags["user-2"]!!
        assertEquals(2, tags.size)
    }

    @Test
    fun tagToggle_independentAcrossPartners() = runTest {
        viewModel.onAction(PartnerRatingAction.OnTagToggle("user-2", PartnerTag.KEPT_PACE))
        viewModel.onAction(PartnerRatingAction.OnTagToggle("user-3", PartnerTag.PUSHED_GROUP))
        assertFalse(PartnerTag.PUSHED_GROUP in (viewModel.state.value.selectedTags["user-2"] ?: emptySet()))
        assertFalse(PartnerTag.KEPT_PACE in (viewModel.state.value.selectedTags["user-3"] ?: emptySet()))
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    @Test
    fun submit_callsRepositoryWithAllPartners() = runTest {
        viewModel.onAction(PartnerRatingAction.OnTagToggle("user-2", PartnerTag.KEPT_PACE))
        viewModel.onAction(PartnerRatingAction.OnSubmitClick)

        assertEquals(1, fakeRepo.submitCallCount)
        val submitted = fakeRepo.lastSubmittedRatings!!
        // Both partners included — user-3 with empty tags
        assertEquals(2, submitted.size)
        assertTrue(PartnerTag.KEPT_PACE in (submitted["user-2"] ?: emptySet()))
        assertTrue((submitted["user-3"] ?: emptySet()).isEmpty())
    }

    @Test
    fun submit_success_emitsSubmitSuccessEvent() = runTest {
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(PartnerRatingAction.OnSubmitClick)
        assertEquals(PartnerRatingEvent.SubmitSuccess, eventDeferred.await())
    }

    @Test
    fun submit_failure_setsError() = runTest {
        fakeRepo.submitResult = Result.Error(RunError.NETWORK_ERROR)
        viewModel.onAction(PartnerRatingAction.OnSubmitClick)
        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun dismissError_clearsError() = runTest {
        fakeRepo.submitResult = Result.Error(RunError.NETWORK_ERROR)
        viewModel.onAction(PartnerRatingAction.OnSubmitClick)
        viewModel.onAction(PartnerRatingAction.OnDismissError)
        assertNull(viewModel.state.value.error)
    }

    // ── Back navigation ───────────────────────────────────────────────────────

    @Test
    fun backClick_emitsNavigateBackEvent() = runTest {
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(PartnerRatingAction.OnBackClick)
        assertEquals(PartnerRatingEvent.NavigateBack, eventDeferred.await())
    }
}
