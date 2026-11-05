package br.com.brunocarvalhs.howmuch.core.analytics.contract

/**
 * Abstrai o backend de analytics (hoje Firebase Analytics) para que quem dispara eventos
 * não dependa diretamente do SDK, e a implementação possa ser trocada sem impacto nas telas.
 */
interface AnalyticsTracker {

    fun trackEvent(name: String, params: Map<String, Any?> = emptyMap())

    fun trackScreenView(screenName: String, screenClass: String? = null)

    fun setUserId(id: String?)

    fun setUserProperty(name: String, value: String?)

    /**
     * Liga ou desliga a coleta inteira (opt-out do usuário em Configurações > Dados). Desligado,
     * nenhum evento sai do aparelho.
     */
    fun setCollectionEnabled(enabled: Boolean)

    /** Última escolha aplicada por [setCollectionEnabled]; `true` até alguém desligar. */
    val isCollectionEnabled: Boolean
}
