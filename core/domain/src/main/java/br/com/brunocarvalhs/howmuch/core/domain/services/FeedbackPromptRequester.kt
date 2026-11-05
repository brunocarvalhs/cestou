package br.com.brunocarvalhs.howmuch.core.domain.services

import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import kotlinx.coroutines.flow.StateFlow

/**
 * Porta pela qual as features avisam que "aconteceu um momento" de valor; quem decide se e quando
 * perguntar é o `:feature:feedback`. Mantida aqui para nenhuma feature depender de outra.
 */
interface FeedbackPromptRequester {

    /** Pedido aguardando um destino estável para ser mostrado; o host o limpa com [consume]. */
    val pending: StateFlow<FeedbackPrompt?>

    /** Conta a sessão. Sem I/O bloqueante. */
    fun onAppForeground()

    /** Registra [moment] e, se a política liberar, publica um pedido em [pending]. */
    fun onMoment(moment: FeedbackMoment)

    /** O host vai mostrar [prompt]: limpa o pendente e registra a exibição. */
    fun consume(prompt: FeedbackPrompt)

    /** `false` com a coleta de analytics desligada ou o recurso desligado remotamente: nada é mostrado. */
    fun isFeedbackEnabled(): Boolean
}
