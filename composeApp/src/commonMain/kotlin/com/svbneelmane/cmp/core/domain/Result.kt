package com.svbneelmane.cmp.core.domain

/**
 * Represents a result of an operation, which can be either [Success] or [Error].
 */
sealed interface Result<out D, out E : Error> {
    /**
     * Represents a successful result containing [data].
     */
    data class Success<out D>(val data: D) : Result<D, Nothing>

    /**
     * Represents a failed result containing an [error].
     */
    data class Error<out E : com.svbneelmane.cmp.core.domain.Error>(val error: E) :
        Result<Nothing, E>
}

/**
 * Transforms the [Success] data using [transform], or returns [Error] unchanged.
 */
inline fun <T, E : Error, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> Result.Error(error)
    }

/**
 * Converts a [Result] to an [EmptyResult], discarding any [Success] data.
 */
fun <T, E : Error> Result<T, E>.asEmptyResult(): EmptyResult<E> = map { }

/**
 * Executes [action] if this is a [Success], then returns the original [Result].
 */
inline fun <T, E : Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> =
    apply { if (this is Result.Success) action(data) }

/**
 * Executes [action] if this is an [Error], then returns the original [Result].
 */
inline fun <T, E : Error> Result<T, E>.onError(action: (E) -> Unit): Result<T, E> =
    apply { if (this is Result.Error) action(error) }

/**
 * Alias for a [Result] with [Unit] as the success type.
 */
typealias EmptyResult<E> = Result<Unit, E>