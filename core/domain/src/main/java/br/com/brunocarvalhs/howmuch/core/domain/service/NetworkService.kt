package br.com.brunocarvalhs.howmuch.core.domain.service

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface NetworkService {

    suspend fun <T : Any> make(
        request: NetworkRequest,
        response: KClass<T>,
        responseType: KType? = null
    ): T?

    fun <T : Any> observe(
        request: NetworkRequest,
        response: KClass<T>,
        responseType: KType? = null
    ): Flow<T?>

    data class NetworkRequest(
        val endpoint: String,
        val payload: Map<String, Any?>? = null,
        val headers: Map<String, String>? = null,
        val query: Map<String, Any>? = null,
        val method: Method
    )

    enum class Method {
        GET,
        POST,
        PUT,
        DELETE
    }

    data class NetworkException(
        val code: Int? = null,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : Exception(message, cause)
}

suspend inline fun <reified T : Any> NetworkService.make(
    request: NetworkService.NetworkRequest
): T? = make(request, T::class, typeOf<T>())

inline fun <reified T : Any> NetworkService.observe(
    request: NetworkService.NetworkRequest
): Flow<T?> = observe(request, T::class, typeOf<T>())
