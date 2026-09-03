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
}
