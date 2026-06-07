package com.example.paceup.shared.rivalengine.domain

/**
 * A rivalry connection between two users.
 *
 * @param id Database row UUID.
 * @param userAId The user who initiated the rival request.
 * @param userBId The user who received the rival request.
 * @param status Connection state: "pending" | "active" | "declined" | "ended".
 * @param userAWins All-time weekly wins for user A.
 * @param userBWins All-time weekly wins for user B.
 * @param currentStreak Who is currently on a winning streak: "user_a" | "user_b" | "tied".
 * @param streakCount Consecutive weeks the current leader has won.
 * @param createdAt ISO-8601 timestamp.
 */
data class Rival(
    val id: String,
    val userAId: String,
    val userBId: String,
    val status: String,
    val userAWins: Int,
    val userBWins: Int,
    val currentStreak: String,
    val streakCount: Int,
    val createdAt: String,
)
