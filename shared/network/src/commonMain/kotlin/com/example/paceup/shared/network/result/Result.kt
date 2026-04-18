package com.example.paceup.shared.network.result

/** Marker interface for all typed error enums used with [Result]. */
interface Error

/**
 * Generic typed result used across all layers — data, domain, presentation, and validation.
 * Never throw exceptions for expected failures; always return [Result.Error].
 */
sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : com.example.paceup.shared.network.result.Error>(val error: E) : Result<Nothing, E>
}

/** Convenience alias for operations that succeed with [Unit] or fail with a typed error. */
typealias EmptyResult<E> = Result<Unit, E>

// --- Extension helpers ---

/** Transforms [Result.Success] data; passes [Result.Error] through unchanged. */
inline fun <T, E : Error, R> Result<T, E>.map(
    transform: (T) -> R
): Result<R, E> = when (this) {
    is Result.Error   -> Result.Error(error)
    is Result.Success -> Result.Success(transform(data))
}

/** Runs [action] on success data; returns the original result for chaining. */
inline fun <T, E : Error> Result<T, E>.onSuccess(
    action: (T) -> Unit
): Result<T, E> = when (this) {
    is Result.Error   -> this
    is Result.Success -> { action(data); this }
}

/** Runs [action] on the error; returns the original result for chaining. */
inline fun <T, E : Error> Result<T, E>.onFailure(
    action: (E) -> Unit
): Result<T, E> = when (this) {
    is Result.Error   -> { action(error); this }
    is Result.Success -> this
}

/** Discards the success value, returning [EmptyResult]. */
fun <T, E : Error> Result<T, E>.asEmptyResult(): EmptyResult<E> = map { }
