package com.example.paceup.shared.network.error

import com.example.paceup.shared.network.result.Error

/**
 * Root sealed interface for all domain-level errors in PaceUp.
 * Feature-specific errors (e.g. [AuthError]) implement this interface.
 */
sealed interface AppError : Error
