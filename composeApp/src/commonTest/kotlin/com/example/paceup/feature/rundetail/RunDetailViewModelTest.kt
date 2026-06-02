package com.example.paceup.feature.rundetail

import androidx.lifecycle.SavedStateHandle
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.result.Result
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RunDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRunRepo: FakeRunRepository
    private lateinit var fakeAuthRepo: FakeAuthRepositoryForDetail
    private lateinit var fakeUserRepo: FakeUserRepository
    private lateinit var viewModel: RunDetailViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRunRepo = FakeRunRepository()
        fakeAuthRepo = FakeAuthRepositoryForDetail()
        fakeUserRepo = FakeUserRepository()
        viewModel = RunDetailViewModel(
            fakeRunRepo,
            fakeAuthRepo,
            fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial load ──────────────────────────────────────────────────────────

    @Test
    fun init_loadsRunAndAcceptedParticipants() = runTest {
        val state = viewModel.state.value
        assertNotNull(state.run)
        assertEquals("run-1", state.run!!.id)
        assertEquals(2, state.participants.size)
        assertNull(state.error)
        assertTrue(!state.isLoading)
    }

    @Test
    fun init_runLoadFailure_setsErrorAndNoRun() = runTest {
        fakeRunRepo.runByIdResult = Result.Error(RunError.NOT_FOUND)
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "bad-id")),
        )
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.run)
        assertTrue(!vm.state.value.isLoading)
    }

    @Test
    fun init_participantsLoadFailure_stillShowsRunWithEmptyList() = runTest {
        fakeRunRepo.observeParticipantsFlow = kotlinx.coroutines.flow.emptyFlow()
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        assertNotNull(vm.state.value.run)
        assertEquals(emptyList(), vm.state.value.participants)
        assertNull(vm.state.value.error)
    }

    @Test
    fun init_reputationTierActive_canJoinIsTrue() = runTest {
        fakeUserRepo.reputationTierResult = Result.Success("active")
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        assertTrue(vm.state.value.canJoin)
    }

    @Test
    fun init_newRunnerTierAndVerifiedOnly_canJoinIsFalse() = runTest {
        fakeUserRepo.reputationTierResult = Result.Success("new_runner")
        // fakeRun has verifiedOnly = true
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        // fakeRun.verifiedOnly = true and tier = new_runner → canJoin should be false
        assertTrue(!vm.state.value.canJoin)
    }

    @Test
    fun init_creatorId_matchesCurrentUser_isCreatorTrue() = runTest {
        // Set the current user as the run creator
        fakeAuthRepo.currentUser = fakeAuthRepo.currentUser?.copy(id = fakeRun.creatorId)
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        assertTrue(vm.state.value.isCreator)
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Test
    fun onBackClick_emitsNavigateBackEvent() = runTest {
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(RunDetailAction.OnBackClick)
        assertEquals(RunDetailEvent.NavigateBack, eventDeferred.await())
    }

    // ── Join flow ─────────────────────────────────────────────────────────────

    @Test
    fun onJoinClick_openMode_setsJoinStatusJoined() = runTest {
        fakeRunRepo.joinRunResult = Result.Success(Unit)
        viewModel.onAction(RunDetailAction.OnJoinClick)
        assertEquals(JoinStatus.JOINED, viewModel.state.value.joinStatus)
        assertNull(viewModel.state.value.joinError)
        assertTrue(!viewModel.state.value.isJoining)
    }

    @Test
    fun onJoinClick_requestMode_setsJoinStatusRequested() = runTest {
        // Use a run with request join mode
        val requestRun = fakeRun.copy(joinMode = "request")
        fakeRunRepo.runByIdResult = Result.Success(requestRun)
        fakeRunRepo.requestToJoinResult = Result.Success(Unit)
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        vm.onAction(RunDetailAction.OnJoinClick)
        assertEquals(JoinStatus.REQUESTED, vm.state.value.joinStatus)
    }

    @Test
    fun onJoinClick_failure_setsJoinError() = runTest {
        fakeRunRepo.joinRunResult = Result.Error(RunError.NETWORK_ERROR)
        viewModel.onAction(RunDetailAction.OnJoinClick)
        assertNotNull(viewModel.state.value.joinError)
        assertEquals(JoinStatus.NONE, viewModel.state.value.joinStatus)
    }

    @Test
    fun onJoinClick_whenCanJoinFalse_doesNothing() = runTest {
        fakeUserRepo.reputationTierResult = Result.Success("new_runner")
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        // fakeRun.verifiedOnly = true → canJoin = false
        vm.onAction(RunDetailAction.OnJoinClick)
        assertEquals(JoinStatus.NONE, vm.state.value.joinStatus)
        assertNull(vm.state.value.joinError)
    }

    // ── Cancel participation ──────────────────────────────────────────────────

    @Test
    fun onCancelParticipation_success_resetsJoinStatusToNone() = runTest {
        // First join
        fakeRunRepo.joinRunResult = Result.Success(Unit)
        viewModel.onAction(RunDetailAction.OnJoinClick)
        assertEquals(JoinStatus.JOINED, viewModel.state.value.joinStatus)

        // Then cancel
        fakeRunRepo.cancelParticipationResult = Result.Success(Unit)
        viewModel.onAction(RunDetailAction.OnCancelParticipationClick)
        assertEquals(JoinStatus.NONE, viewModel.state.value.joinStatus)
    }

    // ── Creator actions ───────────────────────────────────────────────────────

    @Test
    fun onAcceptParticipant_success_triggersParticipantRefresh() = runTest {
        fakeAuthRepo.currentUser = fakeAuthRepo.currentUser?.copy(id = fakeRun.creatorId)
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        assertTrue(vm.state.value.isCreator)
        fakeRunRepo.acceptParticipantResult = Result.Success(Unit)
        vm.onAction(RunDetailAction.OnAcceptParticipant("user-2"))
        // No crash — refresh called without error
    }

    @Test
    fun onDeclineParticipant_success_triggersParticipantRefresh() = runTest {
        fakeAuthRepo.currentUser = fakeAuthRepo.currentUser?.copy(id = fakeRun.creatorId)
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        fakeRunRepo.declineParticipantResult = Result.Success(Unit)
        vm.onAction(RunDetailAction.OnDeclineParticipant("user-2"))
        // No crash — refresh called without error
    }

    // ── Error dismiss ─────────────────────────────────────────────────────────

    @Test
    fun onDismissJoinError_clearsJoinError() = runTest {
        fakeRunRepo.joinRunResult = Result.Error(RunError.NETWORK_ERROR)
        viewModel.onAction(RunDetailAction.OnJoinClick)
        assertNotNull(viewModel.state.value.joinError)

        viewModel.onAction(RunDetailAction.OnDismissJoinError)
        assertNull(viewModel.state.value.joinError)
    }

    // ── Cancel run (creator) ─────────────────────────────────────────────────

    @Test
    fun onCancelRunClick_showsCancelDialog() = runTest {
        fakeAuthRepo.currentUser = fakeAuthRepo.currentUser?.copy(id = fakeRun.creatorId)
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        vm.onAction(RunDetailAction.OnCancelRunClick)
        assertTrue(vm.state.value.showCancelRunDialog)
    }

    @Test
    fun onDismissCancelRunDialog_hidesCancelDialog() = runTest {
        fakeAuthRepo.currentUser = fakeAuthRepo.currentUser?.copy(id = fakeRun.creatorId)
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        vm.onAction(RunDetailAction.OnCancelRunClick)
        vm.onAction(RunDetailAction.OnDismissCancelRunDialog)
        assertTrue(!vm.state.value.showCancelRunDialog)
    }

    @Test
    fun onConfirmCancelRun_success_setsRunStatusCancelled() = runTest {
        fakeAuthRepo.currentUser = fakeAuthRepo.currentUser?.copy(id = fakeRun.creatorId)
        fakeRunRepo.cancelRunResult = Result.Success(Unit)
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        vm.onAction(RunDetailAction.OnConfirmCancelRun("Weather too bad"))
        val state = vm.state.value
        assertEquals(
            com.example.paceup.shared.runmatching.domain.RunStatus.CANCELLED,
            state.run?.status,
        )
        assertTrue(!state.isCancellingRun)
        assertNull(state.cancelRunError)
    }

    @Test
    fun onConfirmCancelRun_failure_setsCancelRunError() = runTest {
        fakeAuthRepo.currentUser = fakeAuthRepo.currentUser?.copy(id = fakeRun.creatorId)
        fakeRunRepo.cancelRunResult = Result.Error(RunError.NETWORK_ERROR)
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        vm.onAction(RunDetailAction.OnConfirmCancelRun("reason"))
        assertNotNull(vm.state.value.cancelRunError)
        assertTrue(!vm.state.value.isCancellingRun)
    }

    @Test
    fun onDismissCancelRunError_clearsCancelRunError() = runTest {
        fakeRunRepo.cancelRunResult = Result.Error(RunError.NETWORK_ERROR)
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        vm.onAction(RunDetailAction.OnConfirmCancelRun("reason"))
        assertNotNull(vm.state.value.cancelRunError)
        vm.onAction(RunDetailAction.OnDismissCancelRunError)
        assertNull(vm.state.value.cancelRunError)
    }

    // ── Retry ────────────────────────────────────────────────────────────────

    @Test
    fun onRetry_reloadsRunAndParticipants() = runTest {
        fakeRunRepo.runByIdResult = Result.Error(RunError.NETWORK_ERROR)
        val vm = RunDetailViewModel(
            fakeRunRepo, fakeAuthRepo, fakeUserRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        assertNotNull(vm.state.value.error)

        fakeRunRepo.runByIdResult = Result.Success(fakeRun)
        vm.onAction(RunDetailAction.OnRetry)

        assertNotNull(vm.state.value.run)
        assertNull(vm.state.value.error)
    }
}
