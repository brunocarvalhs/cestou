package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Isola a construção do [HttpClient] usado por [OpenRouterAiAgent] atrás de uma interface
 * injetável, para que o agente seja testável sem abrir uma conexão HTTP real (sdd-026, A-03) —
 * antes, o client era montado inline dentro do próprio agente, o que o excluía do gate de
 * cobertura do Kover (`howmuch.kover.gradle.kts`).
 */
internal interface OpenRouterHttpClientFactory {

    /**
     * Monta um [HttpClient] configurado com timeout estendido (ver [OpenRouterHttpClientFactoryImpl]).
     * Recebe [json] em vez de construir sua própria instância de [Json] porque [OpenRouterAiAgent]
     * reaproveita o mesmo [Json] tanto para configurar o `ContentNegotiation` deste client quanto
     * para `encode`/`decode` manual de `ChatRequest`/`ChatResponse` fora do client — uma única
     * configuração de serialização para as duas pontas, em vez de duas que podem divergir.
     */
    fun create(json: Json): HttpClient
}

internal class OpenRouterHttpClientFactoryImpl @Inject constructor() : OpenRouterHttpClientFactory {

    override fun create(json: Json): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        // Free-tier OpenRouter models can take a while to respond (routing through "auto",
        // cold-starting a provider, etc.) — the engine's short default timeout was causing
        // real requests to be cut off as SocketTimeoutException before they finished.
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }
    }

    companion object {
        private const val REQUEST_TIMEOUT_MS = 45_000L
        private const val CONNECT_TIMEOUT_MS = 15_000L
    }
}
