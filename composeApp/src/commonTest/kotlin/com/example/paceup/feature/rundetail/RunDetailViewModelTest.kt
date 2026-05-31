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
    private lateinit var fakeRepo: FakeRunRepository
    private lateinit var viewModel: RunDetailViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeRunRepository()
        viewModel = RunDetailViewModel(fakeRepo, SavedStateHandle(mapOf("runId" to "run-1")))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsRunAndParticipants() = runTest {
        val state = viewModel.state.value
        assertNotNull(state.run)
        assertEquals("run-1", state.run!!.id)
        assertEquals(2, state.participants.size)
        assertNull(state.error)
        assertTrue(!state.isLoading)
    }

    @Test
    fun init_runLoadSuccess_setsRunInState() = runTest {
        assertEquals(fakeRun, viewModel.state.value.run)
    }

    @Test
    fun init_participantsLoadSuccess_setsParticipantsInState() = runTest {
        assertEquals(fakeParticipants, viewModel.state.value.participants)
    }

    @Test
    fun init_runLoadFailure_setsErrorAndNoRun() = runTest {
        fakeRepo.runByIdResult = Result.Error(RunError.NOT_FOUND)
        val vm = RunDetailViewModel(fakeRepo, SavedStateHandle(mapOf("runId" to "bad-id")))

        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.run)
        assertTrue(!vm.state.value.isLoading)
    }

    @Test
    fun init_participantsLoadFailure_stillShowsRunWithEmptyList() = runTest {
        fakeRepo.participantsResult = Result.Error(RunError.NETWORK_ERROR)
        val vm = RunDetailViewModel(fakeRepo, SavedStateHandle(mapOf("runId" to "run-1")))

        assertNotNull(vm.state.value.run)
        assertEquals(emptyList(), vm.state.value.participants)
        assertNull(vm.state.value.error)
    }

    @Test
    fun onBackClick_emitsNavigateBackEvent() = runTest {
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(RunDetailAction.OnBackClick)
        assertEquals(RunDetailEvent.NavigateBack, eventDeferred.await())
    }

    @Test
    fun onJoinClick_emitsJoinRunEventWithCorrectId() = runTest {
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(RunDetailAction.OnJoinClick)
        val event = eventDeferred.await()
        assertTrue(event is RunDetailEvent.JoinRun)
        assertEquals("run-1", event.runId)
    }

    @Test
    fun onJoinClick_beforeRunLoaded_doesNotEmitEvent() = runTest {
        // Repo returns error so run is null
        fakeRepo.runByIdResult = Result.Error(RunError.NOT_FOUND)
        val vm = RunDetailViewModel(fakeRepo, SavedStateHandle(mapOf("runId" to "bad-id")))

        var eventEmitted = false
        val job = async {
            vm.events.collect { eventEmitted = true }
        }
        vm.onAction(RunDetailAction.OnJoinClick)
        // Give coroutine a chance to run
        job.cancel()
        assertTrue(!eventEmitted)
    }

    @Test
    fun onRetry_reloadsRunAndParticipants() = runTest {
        // First load fails
        fakeRepo.runByIdResult = Result.Error(RunError.NETWORK_ERROR)
        val vm = RunDetailViewModel(fakeRepo, SavedStateHandle(mapOf("runId" to "run-1")))
        assertNotNull(vm.state.value.error)

        // Fix the repo and retry
        fakeRepo.runByIdResult = Result.Success(fakeRun)
        vm.onAction(RunDetailAction.OnRetry)

        assertNotNull(vm.state.value.run)
        assertNull(vm.state.value.error)
    }
}
