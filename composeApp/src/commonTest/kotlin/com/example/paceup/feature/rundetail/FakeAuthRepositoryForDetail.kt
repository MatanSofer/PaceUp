package com.example.paceup.feature.rundetail

import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.auth.domain.AuthSession
import com.example.paceup.shared.auth.domain.AuthUser
import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result

class FakeAuthRepositoryForDetail : AuthRepository {

    var currentUser: AuthUser? = AuthUser(id = "user-99", email = "runner@example.com")

    override suspend fun getCurrentUser(): Result<AuthUser?, AuthError> =
        Result.Success(currentUser)

    override suspend fun getSession(): Result<AuthSession?, AuthError> =
        Result.Success(currentUser?.let { AuthSession(userId = it.id, accessToken = "token") })

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser, AuthError> =
        Result.Error(AuthError.UNKNOWN)

    override suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser, AuthError> =
        Result.Error(AuthError.UNKNOWN)

    override suspend fun signInWithGoogle(): Result<AuthUser, AuthError> =
        Result.Error(AuthError.OAUTH_FAILED)

    override suspend fun signInWithApple(): Result<AuthUser, AuthError> =
        Result.Error(AuthError.OAUTH_FAILED)

    override suspend fun signOut(): EmptyResult<AuthError> = Result.Success(Unit)
}
