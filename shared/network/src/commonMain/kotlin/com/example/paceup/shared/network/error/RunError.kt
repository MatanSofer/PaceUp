package com.example.paceup.shared.network.error

/** Typed errors for run discovery and management operations. */
enum class RunError : AppError {
    NOT_FOUND,
    NETWORK_ERROR,
    UNKNOWN,
}
