package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import java.io.IOException

/**
 * User-facing copy for a failed [AiAgent.sendMessage] call, shared by every provider.
 *
 * A wrong or generic message here erodes trust fast — a shopper who sees the same "something
 * went wrong" every time can't tell a flaky connection (retry in a bit) from the assistant
 * failing to understand them (rephrase). We only distinguish those two cases, and never leak
 * provider/implementation names (OpenRouter, Gemini) into what the user reads.
 */
internal fun aiErrorMessageFor(error: Throwable): String = if (error is IOException) {
    "Não consegui me conectar à internet agora. Verifique sua conexão e tente novamente em instantes."
} else {
    "Não consegui entender ou processar seu pedido agora. Pode tentar reformular a mensagem?"
}
