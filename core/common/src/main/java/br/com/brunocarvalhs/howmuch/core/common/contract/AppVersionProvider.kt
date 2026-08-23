package br.com.brunocarvalhs.howmuch.core.common.contract

/**
 * Expõe o versionName do app instalado para quem precisa decidir comportamento por versão
 * (ex: escopo de feature flags), sem depender do BuildConfig do módulo :app.
 */
interface AppVersionProvider {

    fun versionName(): String
}
