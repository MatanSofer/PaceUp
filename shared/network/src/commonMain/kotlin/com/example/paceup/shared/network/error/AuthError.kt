package com.example.paceup.shared.network.error

/** Typed errors for authentication and session operations. */
enum class AuthError : AppError {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    EMAIL_NOT_CONFIRMED,
    EMAIL_RATE_LIMITED,
    SESSION_EXPIRED,
    UNAUTHORIZED,
    OAUTH_CANCELLED,
    OAUTH_FAILED,
    UNKNOWN
}
