package br.com.brunocarvalhs.howmuch.core.data.cloud

import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

@Singleton
class CloudNetwork @Inject constructor(
    private val client: HttpClient
) : NetworkService {

    override suspend fun <T : Any> make(
        request: NetworkService.NetworkRequest,
        response: KClass<T>,
        responseType: KType?
    ): T? {
        return try {
            val httpResponse = client.request(request.endpoint) {
                method = when (request.method) {
                    NetworkService.Method.GET -> HttpMethod.Get
                    NetworkService.Method.POST -> HttpMethod.Post
                    NetworkService.Method.PUT -> HttpMethod.Put
                    NetworkService.Method.DELETE -> HttpMethod.Delete
                }

                request.headers?.forEach { (key, value) ->
                    header(key, value)
                }

                request.query?.forEach { (key, value) ->
                    parameter(key, value)
                }

                request.payload?.let {
                    contentType(ContentType.Application.Json)
                    setBody(it)
                }
            }

            if (httpResponse.status.isSuccess()) {
                val typeInfo = responseType?.let {
                    TypeInfo(response, it.javaType, it)
                } ?: TypeInfo(response, response.java)
                httpResponse.body(typeInfo) as? T
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun <T : Any> observe(
        request: NetworkService.NetworkRequest,
        response: KClass<T>,
        responseType: KType?
    ): Flow<T?> = flow {
        emit(make(request, response, responseType))
    }
}
