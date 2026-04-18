package com.example.paceup.shared.network.error

/** Typed errors for all remote/network operations. Maps directly from HTTP status codes and transport failures. */
enum class NetworkError : AppError {
    BAD_REQUEST,
    REQUEST_TIMEOUT,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    TOO_MANY_REQUESTS,
    NO_INTERNET,
    PAYLOAD_TOO_LARGE,
    SERVER_ERROR,
    SERVICE_UNAVAILABLE,
    SERIALIZATION,
    UNKNOWN
}
