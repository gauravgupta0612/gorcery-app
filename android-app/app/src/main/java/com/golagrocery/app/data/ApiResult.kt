package com.golagrocery.app.data

import com.golagrocery.app.data.remote.ErrorBody
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

sealed interface ApiResult<out T> {
    data class Ok<T>(val data: T) : ApiResult<T>
    data class Err(val message: String, val code: String? = null, val status: Int? = null, val fields: Map<String, String>? = null) : ApiResult<Nothing> {
        val isNetwork get() = code == NETWORK
        val isUnauthorized get() = status == 401
    }
    companion object { const val NETWORK = "NETWORK" }
}

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** Runs an API call and converts every failure into an [ApiResult.Err] with a user-presentable message. */
suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Ok(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val body = raw?.let { runCatching { errorJson.decodeFromString(ErrorBody.serializer(), it) }.getOrNull() }
    val fields = body?.fields
    val message = body?.message
    val msg = when {
        !fields.isNullOrEmpty() -> fields.values.first()
        !message.isNullOrBlank() -> message
        e.code() == 401 -> "Please log in again"
        else -> "Request failed (${e.code()})"
    }
    ApiResult.Err(msg, body?.code, e.code(), body?.fields)
} catch (e: IOException) {
    ApiResult.Err("Cannot reach the server. Check your connection.", ApiResult.NETWORK)
} catch (e: kotlinx.serialization.SerializationException) {
    ApiResult.Err("Unexpected response from server", "PARSE")
}

inline fun <T, R> ApiResult<T>.map(f: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Ok -> ApiResult.Ok(f(data))
    is ApiResult.Err -> this
}
