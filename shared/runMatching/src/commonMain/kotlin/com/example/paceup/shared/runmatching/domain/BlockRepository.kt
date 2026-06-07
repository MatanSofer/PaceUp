package com.example.paceup.shared.runmatching.domain

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result

/** Manages block/unblock actions and the blocked-users list. Spec §6.1. */
interface BlockRepository {

    /** Blocks [targetUserId]. Mutual invisibility is enforced server-side via RLS. */
    suspend fun blockUser(targetUserId: String): EmptyResult<AppError>

    /** Removes the block placed by the current user on [targetUserId]. */
    suspend fun unblockUser(targetUserId: String): EmptyResult<AppError>

    /**
     * Returns true if the current user has blocked [targetUserId].
     * Does NOT check the reverse direction — use [fn_is_blocked] on the server for that.
     */
    suspend fun isBlockedByMe(targetUserId: String): Result<Boolean, AppError>

    /** Returns the list of users the current user has blocked, ordered by most-recently blocked. */
    suspend fun getBlockedUsers(): Result<List<UserSummary>, AppError>
}
