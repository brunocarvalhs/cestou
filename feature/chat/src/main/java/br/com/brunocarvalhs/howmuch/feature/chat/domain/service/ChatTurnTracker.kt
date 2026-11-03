package br.com.brunocarvalhs.howmuch.feature.chat.domain.service

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * Turnos da IA em andamento, por conversa (`sessionId` ou, sem sessão, `shoppingId`).
 *
 * O turno roda fora do `viewModelScope` e sobrevive ao fechamento do chat; um chat reaberto
 * consulta este rastreador para saber que ainda há resposta a caminho. Público porque o
 * `AiChatViewModel` o recebe por construtor e `:app` o usa para cancelar turnos no logout.
 */
interface ChatTurnTracker {

    /** Chaves das conversas com turno em andamento. */
    val inFlight: StateFlow<Set<String>>

    fun begin(key: String, job: Job)

    fun end(key: String)

    /** Cancela todos os turnos, ex. ao trocar de conta, para a resposta não cair no histórico errado. */
    fun cancelAll()
}
