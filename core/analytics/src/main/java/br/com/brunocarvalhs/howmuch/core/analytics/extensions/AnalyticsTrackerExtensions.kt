package br.com.brunocarvalhs.howmuch.core.analytics.extensions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams

/**
 * Clique em controle relevante. [elementId] segue `<tela>.<elemento>` (ex.: `cart.share`) e vem
 * de constantes, nunca de texto digitado pelo usuário.
 */
fun AnalyticsTracker.trackAction(elementId: String, extra: Map<String, Any?> = emptyMap()) {
    trackEvent(AnalyticsEvents.UI_ACTION, extra + (AnalyticsParams.ELEMENT_ID to elementId))
}

/**
 * Falha de uma operação. Só a classe da exceção vai como [AnalyticsParams.ERROR_TYPE]: a
 * `message` pode conter dado pessoal (AGENTS.md §5).
 */
fun AnalyticsTracker.trackError(operation: String, error: Throwable) {
    trackEvent(
        AnalyticsEvents.OPERATION_FAILED,
        mapOf(
            AnalyticsParams.OPERATION to operation,
            AnalyticsParams.ERROR_TYPE to (error::class.simpleName ?: "Unknown")
        )
    )
}
