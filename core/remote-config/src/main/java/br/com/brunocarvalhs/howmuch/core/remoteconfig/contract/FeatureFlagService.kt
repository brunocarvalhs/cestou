package br.com.brunocarvalhs.howmuch.core.remoteconfig.contract

/**
 * Liga/desliga funcionalidades remotamente. A implementação (hoje Firebase Remote Config)
 * pode ser trocada sem alterar quem consulta uma flag.
 */
interface FeatureFlagService {

    fun isEnabled(key: String, default: Boolean = false): Boolean
}
