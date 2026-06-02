package com.example.paceup.feature.runchat

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.ChatMessage
import com.example.paceup.shared.runmatching.domain.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow

val fakeMessages = listOf(
    ChatMessage(
        id = "msg-1",
        runId = "run-1",
        userId = "user-2",
        senderName = "Maya Cohen",
        content = "See you at Gordon Beach!",
        createdAt = "2026-06-01T06:30:00Z",
    ),
    ChatMessage(
        id = "msg-2",
        runId = "run-1",
        userId = "user-3",
        senderName = "Dan Levi",
        content = "I'll be there at 7. Wearing the red cap.",
        createdAt = "2026-06-01T06:45:00Z",
    ),
)

class FakeChatRepository : ChatRepository {

    var recentMessagesResult: Result<List<ChatMessage>, AppError> = Result.Success(fakeMessages)
    var sendMessageResult: Result<Unit, AppError> = Result.Success(Unit)
    val incomingMessages = MutableSharedFlow<ChatMessage>()

    override suspend fun getRecentMessages(runId: String): Result<List<ChatMessage>, AppError> =
        recentMessagesResult

    override fun observeNewMessages(runId: String): Flow<ChatMessage> = incomingMessages

    override suspend fun sendMessage(runId: String, content: String): Result<Unit, AppError> =
        sendMessageResult
}
