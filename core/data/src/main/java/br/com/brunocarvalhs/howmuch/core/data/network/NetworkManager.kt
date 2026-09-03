package br.com.brunocarvalhs.howmuch.core.data.network

import br.com.brunocarvalhs.howmuch.core.data.security.CryptoManager
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import timber.log.Timber
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.KType

class NetworkManager @Inject constructor(
    private val firebaseFirestoreManager: FirebaseFirestoreManager,
    private val cryptoManager: CryptoManager,
    private val compatibilityConverter: CompatibilityConverter,
    private val networkLogger: NetworkLogger
) : NetworkService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override suspend fun <T : Any> make(
        request: NetworkService.NetworkRequest,
        response: KClass<T>,
        responseType: KType?
    ): T? {
        val startTime = System.currentTimeMillis()

        val finalEndpoint = getEndpoint(request.endpoint)
        val finalPayload = getPayload(request.payload)

        networkLogger.request(
            method = request.method.name,
            endpoint = finalEndpoint,
            query = request.query,
            payload = finalPayload,
            response = response
        )

        return try {
            val result = firebaseFirestoreManager.execute(
                endpoint = finalEndpoint,
                method = request.method,
                data = finalPayload,
                query = request.query,
            )

            networkLogger.rawResponse(
                endpoint = finalEndpoint,
                body = result
            )

            val responseBody = getResponse(result, response, responseType)
            networkLogger.success(
                endpoint = finalEndpoint,
                elapsed = System.currentTimeMillis() - startTime,
                body = responseBody
            )
            responseBody
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            networkLogger.failure(
                endpoint = finalEndpoint,
                elapsed = System.currentTimeMillis() - startTime,
                throwable = e
            )
            null
        }
    }

    override fun <T : Any> observe(
        request: NetworkService.NetworkRequest,
        response: KClass<T>,
        responseType: KType?
    ): Flow<T?> {
        val finalEndpoint = getEndpoint(request.endpoint)

        return firebaseFirestoreManager.observe(
            endpoint = finalEndpoint,
            query = request.query
        ).map { result ->
            getResponse(result, response, responseType)
        }
    }

    private fun getPayload(payload: Map<String, Any?>?): Map<String, Any?>? {
        return payload
    }

    private fun getEndpoint(endpoint: String): String = endpoint

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getResponse(result: Any?, response: KClass<T>, responseType: KType?): T? {
        val data = result ?: return null

        return try {
            val serializer = getSerializer(response, responseType)
            val decryptedData = decryptData(data)
            val jsonElement = compatibilityConverter.toJsonElement(decryptedData)

            networkLogger.responseProcessing(
                response = response,
                raw = data,
                decrypted = decryptedData,
                json = jsonElement
            )

            json.decodeFromJsonElement(serializer, jsonElement) as? T
        } catch (e: SerializationException) {
            networkLogger.serializationError(response, data, e)
            handleFallback(data, response)
        } catch (e: IllegalArgumentException) {
            networkLogger.argumentError(response, data, e)
            handleFallback(data, response)
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun <T : Any> getSerializer(response: KClass<T>, responseType: KType?): KSerializer<Any?> {
        return (responseType?.let { json.serializersModule.serializer(it) }
            ?: json.serializersModule.serializer(response.java)) as KSerializer<Any?>
    }

    @Suppress("UNCHECKED_CAST")
    private fun decryptData(data: Any): Any {
        return when (data) {
            is Map<*, *> -> cryptoManager.decryptMap(data as Map<String, Any>, EXCLUDED_KEYS)
            is List<*> -> data.map { item ->
                if (item is Map<*, *>) {
                    cryptoManager.decryptMap(item as Map<String, Any>, EXCLUDED_KEYS)
                } else {
                    item
                }
            }
            else -> data
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> handleFallback(data: Any?, response: KClass<T>): T? {
        return if (data is List<*> && response.java.isArray) {
            runCatching { compatibilityConverter.listToTypedArray(data, response.java) as? T }.getOrNull()
        } else {
            null
        }
    }

    companion object {
        private val EXCLUDED_KEYS = setOf("")
    }
}
