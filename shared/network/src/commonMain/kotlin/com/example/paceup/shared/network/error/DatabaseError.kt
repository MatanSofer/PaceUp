package com.example.paceup.shared.network.error

/** Typed errors for local SQLDelight database operations. */
enum class DatabaseError : AppError {
    DISK_FULL,
    NOT_FOUND,
    UNKNOWN
}
