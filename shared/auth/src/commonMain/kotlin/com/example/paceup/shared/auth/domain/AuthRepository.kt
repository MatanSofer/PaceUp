package com.example.paceup.shared.auth.domain

import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result

/** Contract for all authentication operations. Pure Kotlin — no framework imports. */
interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser, AuthError>
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser, AuthError>
    /** Google OAuth — requires platform browser flow. Returns [AuthError.OAUTH_FAILED] until wired. */
    suspend fun signInWithGoogle(): Result<AuthUser, AuthError>
    /** Apple OAuth — requires platform browser flow. Returns [AuthError.OAUTH_FAILED] until wired. */
    suspend fun signInWithApple(): Result<AuthUser, AuthError>
    suspend fun signOut(): EmptyResult<AuthError>
    suspend fun getCurrentUser(): Result<AuthUser?, AuthError>
    suspend fun getSession(): Result<AuthSession?, AuthError>
}
