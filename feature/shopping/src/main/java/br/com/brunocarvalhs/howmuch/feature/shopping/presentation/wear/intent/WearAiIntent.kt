package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent

/** Ações da entrada rápida de IA no relógio. */
sealed interface WearAiIntent {
    data class Send(val prompt: String) : WearAiIntent

    /** Aprova ou recusa a ação pendente. Só vale no estado `Confirming`. */
    data class Decide(val approved: Boolean) : WearAiIntent

    /** Volta à tela inicial depois de uma resposta ou falha. Não interrompe uma espera. */
    data object Reset : WearAiIntent
}
