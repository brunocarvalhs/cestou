package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state

/** Entrada rápida de IA no relógio. Cada estado é uma tela inteira, nunca um misto. */
sealed interface WearAiUiState {
    /** Nada enviado ainda, ou a última resposta já foi lida. */
    data object Idle : WearAiUiState

    /** Prompt enviado; esperando o celular responder. */
    data object Waiting : WearAiUiState

    /** O agente pediu para executar uma ação de escrita; o usuário confirma ou recusa no relógio. */
    data class Confirming(val requestId: String, val actionId: String, val summary: String) : WearAiUiState

    /** Resposta curta do agente. */
    data class Answer(val text: String) : WearAiUiState

    /**
     * Não deu para responder. [reason] é um nome de `AiFailure` ou um dos motivos do relógio
     * ([PHONE_UNAVAILABLE], [TIMEOUT]).
     */
    data class Failed(val reason: String) : WearAiUiState

    companion object {
        const val PHONE_UNAVAILABLE = "PHONE_UNAVAILABLE"
        const val TIMEOUT = "TIMEOUT"
    }
}
