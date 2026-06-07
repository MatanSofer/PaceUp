package com.example.paceup.feature.runchat

import androidx.lifecycle.SavedStateHandle
import com.example.paceup.feature.rundetail.FakeAuthRepositoryForDetail
import com.example.paceup.feature.rundetail.FakeReportRepository
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.ChatMessage
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
class RunChatViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeChatRepo: FakeChatRepository
    private lateinit var fakeAuthRepo: FakeAuthRepositoryForDetail
    private lateinit var fakeReportRepo: FakeReportRepository
    private lateinit var viewModel: RunChatViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeChatRepo = FakeChatRepository()
        fakeAuthRepo = FakeAuthRepositoryForDetail()
        fakeReportRepo = FakeReportRepository()
        viewModel = RunChatViewModel(
            fakeChatRepo,
            fakeAuthRepo,
            fakeReportRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial load ──────────────────────────────────────────────────────────

    @Test
    fun init_loadsRecentMessages() = runTest {
        val state = viewModel.state.value
        assertEquals(2, state.messages.size)
        assertEquals("msg-1", state.messages[0].id)
        assertTrue(!state.isLoading)
        assertNull(state.error)
        assertTrue(!state.isOffline)
    }

    @Test
    fun init_loadFailure_setsOfflineFlag() = runTest {
        fakeChatRepo.recentMessagesResult = Result.Error(RunError.NETWORK_ERROR)
        val vm = RunChatViewModel(
            fakeChatRepo,
            fakeAuthRepo,
            fakeReportRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        assertTrue(vm.state.value.isOffline)
        assertTrue(!vm.state.value.isLoading)
    }

    // ── Realtime ──────────────────────────────────────────────────────────────

    @Test
    fun realtimeMessage_appendedToList() = runTest {
        val newMsg = ChatMessage(
            id = "msg-3",
            runId = "run-1",
            userId = "user-4",
            senderName = "Noa Bar",
            content = "Running late, 5 min!",
            createdAt = "2026-06-01T06:55:00Z",
        )
        fakeChatRepo.incomingMessages.emit(newMsg)
        assertEquals(3, viewModel.state.value.messages.size)
        assertEquals("msg-3", viewModel.state.value.messages.last().id)
    }

    @Test
    fun realtimeMessage_duplicate_notAppended() = runTest {
        // Emit a message whose id already exists in the initial list
        fakeChatRepo.incomingMessages.emit(fakeMessages[0])
        assertEquals(2, viewModel.state.value.messages.size)
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Test
    fun onInputChange_updatesInput() = runTest {
        viewModel.onAction(RunChatAction.OnInputChange("Hello!"))
        assertEquals("Hello!", viewModel.state.value.input)
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    @Test
    fun onSendClick_success_clearsInput() = runTest {
        viewModel.onAction(RunChatAction.OnInputChange("Let's go!"))
        viewModel.onAction(RunChatAction.OnSendClick)
        assertEquals("", viewModel.state.value.input)
        assertTrue(!viewModel.state.value.isSending)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun onSendClick_blankInput_doesNothing() = runTest {
        viewModel.onAction(RunChatAction.OnInputChange("   "))
        viewModel.onAction(RunChatAction.OnSendClick)
        assertTrue(!viewModel.state.value.isSending)
    }

    @Test
    fun onSendClick_failure_setsError() = runTest {
        fakeChatRepo.sendMessageResult = Result.Error(RunError.NETWORK_ERROR)
        viewModel.onAction(RunChatAction.OnInputChange("Hello"))
        viewModel.onAction(RunChatAction.OnSendClick)
        assertNotNull(viewModel.state.value.error)
        assertTrue(!viewModel.state.value.isSending)
    }

    @Test
    fun onDismissError_clearsError() = runTest {
        fakeChatRepo.sendMessageResult = Result.Error(RunError.NETWORK_ERROR)
        viewModel.onAction(RunChatAction.OnInputChange("Hello"))
        viewModel.onAction(RunChatAction.OnSendClick)
        assertNotNull(viewModel.state.value.error)
        viewModel.onAction(RunChatAction.OnDismissError)
        assertNull(viewModel.state.value.error)
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Test
    fun onBackClick_emitsNavigateBackEvent() = runTest {
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(RunChatAction.OnBackClick)
        assertEquals(RunChatEvent.NavigateBack, eventDeferred.await())
    }

    // ── Retry ─────────────────────────────────────────────────────────────────

    @Test
    fun onRetry_reloadsMessages() = runTest {
        fakeChatRepo.recentMessagesResult = Result.Error(RunError.NETWORK_ERROR)
        val vm = RunChatViewModel(
            fakeChatRepo,
            fakeAuthRepo,
            fakeReportRepo,
            SavedStateHandle(mapOf("runId" to "run-1")),
        )
        assertTrue(vm.state.value.isOffline)

        fakeChatRepo.recentMessagesResult = Result.Success(fakeMessages)
        vm.onAction(RunChatAction.OnRetry)
        assertEquals(2, vm.state.value.messages.size)
        assertTrue(!vm.state.value.isOffline)
    }
}
