package br.com.brunocarvalhs.howmuch.core.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AiModelRepositoryImplTest {

    @Test
    fun `getAvailableModels returns mapped models on success`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"data": [{"id": "openai/gpt-4o:free", "name": "GPT-4o", "pricing": {"prompt": "0.0", "completion": "0.0"}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val repository = AiModelRepositoryImpl(client)

        val models = repository.getAvailableModels()
        assertEquals(1, models.size)
        assertEquals("openai/gpt-4o:free", models[0].id)
        assertEquals("GPT-4o", models[0].name)
        assertEquals("Openai", models[0].vendor)
        assertEquals(true, models[0].isFree)
    }

    @Test
    fun `getAvailableModels returns empty list on network exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val repository = AiModelRepositoryImpl(client)

        val models = repository.getAvailableModels()
        assertEquals(0, models.size)
    }
}
