package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import kotlinx.serialization.json.Json
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Cobre a construção do [io.ktor.client.HttpClient] extraída de [OpenRouterAgent] (sdd-026,
 * A-03) — antes inline e não testável sem abrir uma conexão real.
 */
class OpenRouterHttpClientFactoryImplTest {

    private val factory = OpenRouterHttpClientFactoryImpl()

    @Test
    fun `create returns a usable HttpClient`() {
        val json = Json { ignoreUnknownKeys = true }

        val client = factory.create(json)

        assertNotNull(client.engine)
        client.close()
    }
}
