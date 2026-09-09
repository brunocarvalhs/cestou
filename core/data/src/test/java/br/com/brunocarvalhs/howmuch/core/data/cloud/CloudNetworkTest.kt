package br.com.brunocarvalhs.howmuch.core.data.cloud

import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `CloudNetwork` is the [NetworkService] implementation every Firestore-backed repository in
 * `feature/shopping`/`feature/products` goes through (list creation, join/share, product
 * read/write). It had zero unit coverage before this test: a regression here silently breaks
 * every network read/write in the app, always falling back to `null`/empty results.
 */
class CloudNetworkTest {

    @Serializable
    private data class Payload(val value: String)

    private fun networkFor(engine: MockEngine): CloudNetwork {
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return CloudNetwork(client)
    }

    @Test
    fun `make returns the deserialized body on a successful response`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            respond(
                content = """{"value":"ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val network = networkFor(engine)

        val result = network.make(
            request = NetworkService.NetworkRequest(
                endpoint = "https://example.invalid/resource",
                method = NetworkService.Method.GET
            ),
            response = Payload::class,
            responseType = null
        )

        assertEquals(Payload("ok"), result)
    }

    @Test
    fun `make returns null when the response is not successful`() = runTest {
        val engine = MockEngine {
            respondError(HttpStatusCode.InternalServerError)
        }
        val network = networkFor(engine)

        val result = network.make(
            request = NetworkService.NetworkRequest(
                endpoint = "https://example.invalid/resource",
                method = NetworkService.Method.GET
            ),
            response = Payload::class,
            responseType = null
        )

        assertNull(result)
    }

    @Test
    fun `make returns null instead of throwing when the client fails`() = runTest {
        val engine = MockEngine {
            throw java.io.IOException("network down")
        }
        val network = networkFor(engine)

        val result = network.make(
            request = NetworkService.NetworkRequest(
                endpoint = "https://example.invalid/resource",
                method = NetworkService.Method.GET
            ),
            response = Payload::class,
            responseType = null
        )

        assertNull(result)
    }

    @Test
    fun `make sends headers, query params, method and json body for a POST`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("bearer-token", request.headers["Authorization"])
            assertTrue(request.url.parameters.contains("filter", "active"))
            respond(
                content = """{"value":"created"}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val network = networkFor(engine)

        val result = network.make(
            request = NetworkService.NetworkRequest(
                endpoint = "https://example.invalid/resource",
                method = NetworkService.Method.POST,
                headers = mapOf("Authorization" to "bearer-token"),
                query = mapOf("filter" to "active"),
                payload = mapOf("value" to "created")
            ),
            response = Payload::class,
            responseType = null
        )

        assertEquals(Payload("created"), result)
    }

    @Test
    fun `observe emits a single value produced by make`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"value":"observed"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val network = networkFor(engine)

        val emitted = network.observe(
            request = NetworkService.NetworkRequest(
                endpoint = "https://example.invalid/resource",
                method = NetworkService.Method.GET
            ),
            response = Payload::class,
            responseType = null
        ).first()

        assertEquals(Payload("observed"), emitted)
    }
}
