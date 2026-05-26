package com.example.paceup.shared.runmatching.data

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.UserRepository
import com.example.paceup.shared.runmatching.domain.UserSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

private const val TAG = "SupabaseUserRepository"
private const val TABLE = "users"

/** Supabase-backed implementation of [UserRepository]. */
class SupabaseUserRepository(private val supabase: SupabaseClient) : UserRepository {

    override suspend fun searchUsers(query: String): Result<List<UserSummary>, AppError> {
        AppLogger.d(TAG, "searchUsers query=$query")
        return runCatching {
            supabase.postgrest[TABLE]
                .select(Columns.raw("id, display_name, avatar_url, pace_zone, show_up_rate")) {
                    filter {
                        ilike("display_name", "%$query%")
                        // Only show non-banned, non-suspended users in search results
                        eq("is_banned", false)
                        eq("is_suspended", false)
                    }
                    limit(50)
                }
                .decodeList<UserSearchDto>()
                .map { it.toDomain() }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                AppLogger.e(TAG, "searchUsers failed: ${e.message}")
                Result.Error(RunError.NETWORK_ERROR)
            }
        )
    }
}
