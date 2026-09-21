package br.com.brunocarvalhs.howmuch.core.data.network

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class NetworkLogger @Inject constructor() {

    fun request(
        method: String,
        endpoint: String,
        query: Any?,
        payload: Any?,
        response: KClass<*>
    ) {
        Timber.tag(TAG).i(
            """
            |--> REQUEST
            |Method   : $method
            |Endpoint : $endpoint
            |Response : ${response.simpleName}
            |Query    : $query
            |Payload  : $payload
            """.trimMargin()
        )
    }

    fun rawResponse(
        endpoint: String,
        body: Any?
    ) {
        Timber.tag(TAG).v(
            """
            |<-- RAW RESPONSE
            |Endpoint : $endpoint
            |Body     : $body
            """.trimMargin()
        )
    }

    fun success(
        endpoint: String,
        elapsed: Long,
        body: Any?
    ) {
        Timber.tag(TAG).i(
            """
            |<-- SUCCESS
            |Endpoint : $endpoint
            |Time     : ${elapsed}ms
            |Body     : $body
            """.trimMargin()
        )
    }

    fun failure(
        endpoint: String,
        elapsed: Long,
        throwable: Throwable
    ) {
        Timber.tag(TAG).e(
            throwable,
            """
            |<-- FAILURE
            |Endpoint : $endpoint
            |Time     : ${elapsed}ms
            """.trimMargin()
        )
    }

    fun responseProcessing(
        response: KClass<*>,
        raw: Any?,
        decrypted: Any?,
        json: Any?
    ) {
        Timber.tag(TAG).v(
            """
            |PROCESSING
            |Type      : ${response.simpleName}
            |Raw       : $raw
            |Decrypted : $decrypted
            |Json      : $json
            """.trimMargin()
        )
    }

    fun serializationError(
        response: KClass<*>,
        raw: Any?,
        throwable: Throwable
    ) {
        Timber.tag(TAG).e(
            throwable,
            """
            |<-- SERIALIZATION ERROR
            |Type     : ${response.simpleName}
            |Raw      : $raw
            |Message  : ${throwable.message}
            """.trimMargin()
        )
    }

    fun argumentError(
        response: KClass<*>,
        raw: Any?,
        throwable: Throwable
    ) {
        Timber.tag(TAG).e(
            throwable,
            """
            |<-- ARGUMENT ERROR
            |Type     : ${response.simpleName}
            |Raw      : $raw
            |Message  : ${throwable.message}
            """.trimMargin()
        )
    }

    companion object {
        private const val TAG = "Network"
    }
}
