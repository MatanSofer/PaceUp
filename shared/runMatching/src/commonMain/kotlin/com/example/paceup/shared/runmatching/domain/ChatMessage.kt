package com.example.paceup.shared.runmatching.domain

/** A single message in a run's group chat (spec §4.6). */
data class ChatMessage(
    val id: String,
    val runId: String,
    val userId: String,
    /** Display name of the sender, joined from the users table. */
    val senderName: String,
    val content: String,
    val createdAt: String,
)
