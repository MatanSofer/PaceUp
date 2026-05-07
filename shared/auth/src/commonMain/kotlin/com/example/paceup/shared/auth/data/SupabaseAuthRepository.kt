package com.example.paceup.shared.auth.data

import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.auth.domain.AuthSession
import com.example.paceup.shared.auth.domain.AuthUser
import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.logger.AppLogger
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException

/** [AuthRepository] backed by Supabase Auth. Session is managed by the Supabase SDK. */
class SupabaseAuthRepository(private val client: SupabaseClient) : AuthRepository {

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<AuthUser, AuthError> {
        AppLogger.d(TAG, "signInWithEmail enter: $email")
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val session = client.auth.currentSessionOrNull()
                ?: return Result.Error(AuthError.UNAUTHORIZED)
            val userId = session.user?.id
                ?: return Result.Error(AuthError.UNAUTHORIZED)
            AppLogger.i(TAG, "signInWithEmail success: $userId")
            Result.Success(AuthUser(id = userId, email = session.user?.email))
        } catch (e: RestException) {
            AppLogger.e(TAG, "signInWithEmail failed: ${e.message}")
            Result.Error(e.toAuthError())
        } catch (e: Exception) {
            AppLogger.e(TAG, "signInWithEmail unexpected: ${e.message}")
            Result.Error(AuthError.UNKNOWN)
        }
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String
    ): Result<AuthUser, AuthError> {
        AppLogger.d(TAG, "signUpWithEmail enter: $email")
        return try {
            val userInfo = client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // userInfo is null when email confirmation is required
            val user = if (userInfo != null) {
                AuthUser(id = userInfo.id, email = userInfo.email)
            } else {
                val session = client.auth.currentSessionOrNull()
                    ?: return Result.Error(AuthError.UNKNOWN)
                val userId = session.user?.id ?: return Result.Error(AuthError.UNKNOWN)
                AuthUser(id = userId, email = session.user?.email)
            }
            AppLogger.i(TAG, "signUpWithEmail success: ${user.id}")
            Result.Success(user)
        } catch (e: RestException) {
            AppLogger.e(TAG, "signUpWithEmail failed: ${e.message}")
            Result.Error(e.toAuthError())
        } catch (e: Exception) {
            AppLogger.e(TAG, "signUpWithEmail unexpected: ${e.message}")
            Result.Error(AuthError.UNKNOWN)
        }
    }

    override suspend fun signInWithGoogle(): Result<AuthUser, AuthError> {
        // TODO(paceup): wire Google OAuth — Chrome Custom Tabs on Android, ASWebAuthenticationSession on iOS
        AppLogger.w(TAG, "signInWithGoogle: browser OAuth not yet wired")
        return Result.Error(AuthError.OAUTH_FAILED)
    }

    override suspend fun signInWithApple(): Result<AuthUser, AuthError> {
        // TODO(paceup): wire Apple OAuth — ASWebAuthenticationSession on iOS, Chrome Custom Tabs on Android
        AppLogger.w(TAG, "signInWithApple: browser OAuth not yet wired")
        return Result.Error(AuthError.OAUTH_FAILED)
    }

    override suspend fun signOut(): EmptyResult<AuthError> {
        AppLogger.d(TAG, "signOut enter")
        return try {
            client.auth.signOut(SignOutScope.LOCAL)
            AppLogger.i(TAG, "signOut success")
            Result.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "signOut failed: ${e.message}")
            Result.Error(AuthError.UNKNOWN)
        }
    }

    override suspend fun getCurrentUser(): Result<AuthUser?, AuthError> {
        AppLogger.d(TAG, "getCurrentUser enter")
        return try {
            val userInfo = client.auth.currentUserOrNull()
            val user = userInfo?.let { AuthUser(id = it.id, email = it.email) }
            AppLogger.d(TAG, "getCurrentUser: ${user?.id ?: "null"}")
            Result.Success(user)
        } catch (e: Exception) {
            AppLogger.e(TAG, "getCurrentUser failed: ${e.message}")
            Result.Error(AuthError.UNKNOWN)
        }
    }

    override suspend fun getSession(): Result<AuthSession?, AuthError> {
        AppLogger.d(TAG, "getSession enter")
        return try {
            val session = client.auth.currentSessionOrNull()
            val authSession = session?.let {
                val userId = it.user?.id ?: return Result.Error(AuthError.SESSION_EXPIRED)
                AuthSession(userId = userId, accessToken = it.accessToken)
            }
            AppLogger.d(TAG, "getSession: ${authSession?.userId ?: "null"}")
            Result.Success(authSession)
        } catch (e: Exception) {
            AppLogger.e(TAG, "getSession failed: ${e.message}")
            Result.Error(AuthError.UNKNOWN)
        }
    }

    private fun RestException.toAuthError(): AuthError = when {
        message?.contains("invalid_credentials", ignoreCase = true) == true -> AuthError.INVALID_CREDENTIALS
        message?.contains("already registered", ignoreCase = true) == true ||
        message?.contains("already_registered", ignoreCase = true) == true -> AuthError.EMAIL_ALREADY_IN_USE
        message?.contains("weak_password", ignoreCase = true) == true -> AuthError.WEAK_PASSWORD
        message?.contains("email_not_confirmed", ignoreCase = true) == true -> AuthError.EMAIL_NOT_CONFIRMED
        message?.contains("over_email_send_rate_limit", ignoreCase = true) == true -> AuthError.EMAIL_RATE_LIMITED
        message?.contains("email rate limit", ignoreCase = true) == true -> AuthError.EMAIL_RATE_LIMITED
        message?.contains("session_expired", ignoreCase = true) == true -> AuthError.SESSION_EXPIRED
        else -> AuthError.UNKNOWN
    }

    private companion object {
        const val TAG = "SupabaseAuthRepository"
    }
}
