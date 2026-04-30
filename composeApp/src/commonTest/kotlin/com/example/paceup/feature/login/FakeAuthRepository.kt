package com.example.paceup.feature.login

import com.example.paceup.shared.auth.domain.AuthRepository
import com.example.paceup.shared.auth.domain.AuthSession
import com.example.paceup.shared.auth.domain.AuthUser
import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result

class FakeAuthRepository : AuthRepository {

    var signInResult: Result<AuthUser, AuthError> =
        Result.Success(AuthUser(id = "user-1", email = "test@example.com"))
    var signUpResult: Result<AuthUser, AuthError> =
        Result.Success(AuthUser(id = "user-1", email = "test@example.com"))
    var googleSignInResult: Result<AuthUser, AuthError> = Result.Error(AuthError.OAUTH_FAILED)
    var appleSignInResult: Result<AuthUser, AuthError> = Result.Error(AuthError.OAUTH_FAILED)
    var shouldReturnError = false

    override suspend fun signInWithEmail(email: String, password: String) = signInResult
    override suspend fun signUpWithEmail(email: String, password: String) = signUpResult
    override suspend fun signInWithGoogle() = googleSignInResult
    override suspend fun signInWithApple() = appleSignInResult

    override suspend fun signOut(): EmptyResult<AuthError> = Result.Success(Unit)

    override suspend fun getCurrentUser(): Result<AuthUser?, AuthError> =
        Result.Success(null)

    override suspend fun getSession(): Result<AuthSession?, AuthError> =
        Result.Success(null)
}
