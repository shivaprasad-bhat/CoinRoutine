package com.svbneelmane.cmp.core.network

import com.svbneelmane.cmp.core.domain.DataError
import com.svbneelmane.cmp.core.domain.Result
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * Executes the given [execute] function to perform an HTTP request safely, mapping exceptions to [DataError.Remote].
 *
 * @param execute A function that returns an [HttpResponse].
 * @return [Result.Success] with the deserialized body of type [T] if the request is successful,
 *         or [Result.Error] with an appropriate [DataError.Remote] if an error occurs.
 *
 * Handles:
 * - [SocketTimeoutException] as [DataError.Remote.REQUEST_TIMEOUT]
 * - [UnresolvedAddressException] as [DataError.Remote.NO_INTERNET]
 * - Any other exception as [DataError.Remote.UNKNOWN]
 */
suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, DataError.Remote> {
    val response = try {
        execute()
    } catch (e: SocketTimeoutException) {
        return Result.Error(DataError.Remote.REQUEST_TIMEOUT)
    } catch (e: UnresolvedAddressException) {
        return Result.Error(DataError.Remote.NO_INTERNET)
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        return Result.Error(DataError.Remote.UNKNOWN)
    }

    return responseToResult<T>(response)
}


/**
 * Converts an [HttpResponse] to a [Result], mapping HTTP status codes and deserialization errors to [DataError.Remote].
 *
 * @param response The HTTP response to process.
 * @return [Result.Success] with the deserialized body of type [T] for successful responses (2xx),
 *         or [Result.Error] with an appropriate [DataError.Remote] for error responses or deserialization failures.
 *
 * Handles:
 * - 2xx: Attempts to deserialize the response body to [T]; returns [SERIALIZATION] error if it fails.
 * - 408: Maps to [REQUEST_TIMEOUT].
 * - 429: Maps to [TOO_MANY_REQUESTS].
 * - 5xx: Maps to [SERVER].
 * - Any other status: Maps to [UNKNOWN].
 */
suspend inline fun <reified T> responseToResult(
    response: HttpResponse
): Result<T, DataError.Remote> {
    return when (response.status.value) {
        in 200..299 -> {
            try {
                val data = response.body<T>()
                Result.Success(data)
            } catch (e: Exception) {
                Result.Error(DataError.Remote.SERIALIZATION)
            }
        }

        408 -> Result.Error(DataError.Remote.REQUEST_TIMEOUT)
        429 -> Result.Error(DataError.Remote.TOO_MANY_REQUESTS)
        in 500..599 -> Result.Error(DataError.Remote.SERVER)
        else -> Result.Error(DataError.Remote.UNKNOWN)
    }
}