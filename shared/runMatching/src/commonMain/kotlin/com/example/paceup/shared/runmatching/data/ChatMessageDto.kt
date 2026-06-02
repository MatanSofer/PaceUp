package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.runmatching.domain.ChatMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape returned when joining run_chat_messages with users (display_name). */
@Serializable
internal data class ChatMessageDto(
    @SerialName("id") val id: String,
    @SerialName("run_id") val runId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("content") val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("users") val users: ChatSenderDto? = null,
)

@Serializable
internal data class ChatSenderDto(
    @SerialName("display_name") val displayName: String? = null,
)

/** Minimal payload for real-time INSERT events — no joined columns available. */
@Serializable
internal data class ChatMessageRealtimeDto(
    @SerialName("id") val id: String,
    @SerialName("run_id") val runId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("content") val content: String,
    @SerialName("created_at") val createdAt: String,
)

/** Payload for inserting a new message into run_chat_messages. */
@Serializable
internal data class SendMessageDto(
    @SerialName("run_id") val runId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("content") val content: String,
)

internal fun ChatMessageDto.toDomain(): ChatMessage = ChatMessage(
    id = id,
    runId = runId,
    userId = userId,
    senderName = users?.displayName ?: "Runner",
    content = content,
    createdAt = createdAt,
)

internal fun ChatMessageRealtimeDto.toDomain(fallbackName: String = "Runner"): ChatMessage = ChatMessage(
    id = id,
    runId = runId,
    userId = userId,
    senderName = fallbackName,
    content = content,
    createdAt = createdAt,
)
