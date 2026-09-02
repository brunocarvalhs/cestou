package br.com.brunocarvalhs.howmuch.core.data.network

import org.junit.Test

class NetworkLoggerTest {

    private val logger = NetworkLogger()

    @Test
    fun `request logs without throwing`() {
        logger.request(
            method = "GET",
            endpoint = "shopping",
            query = mapOf("users" to "user-1"),
            payload = mapOf("title" to "List"),
            response = String::class
        )
    }

    @Test
    fun `rawResponse logs without throwing`() {
        logger.rawResponse(endpoint = "shopping", body = mapOf("id" to "1"))
    }

    @Test
    fun `success logs without throwing`() {
        logger.success(endpoint = "shopping", elapsed = 42L, body = listOf("1", "2"))
    }

    @Test
    fun `failure logs without throwing`() {
        logger.failure(endpoint = "shopping", elapsed = 42L, throwable = RuntimeException("boom"))
    }

    @Test
    fun `responseProcessing logs without throwing`() {
        logger.responseProcessing(
            response = String::class,
            raw = "raw",
            decrypted = "decrypted",
            json = "{}"
        )
    }

    @Test
    fun `serializationError logs without throwing`() {
        logger.serializationError(
            response = String::class,
            raw = "raw",
            throwable = IllegalStateException("bad json")
        )
    }

    @Test
    fun `argumentError logs without throwing`() {
        logger.argumentError(
            response = String::class,
            raw = "raw",
            throwable = IllegalArgumentException("bad arg")
        )
    }

    @Test
    fun `all methods tolerate null-ish payloads`() {
        logger.request(method = "POST", endpoint = "shopping", query = null, payload = null, response = Unit::class)
        logger.rawResponse(endpoint = "shopping", body = null)
        logger.success(endpoint = "shopping", elapsed = 0L, body = null)
        logger.responseProcessing(response = Unit::class, raw = null, decrypted = null, json = null)
    }
}
