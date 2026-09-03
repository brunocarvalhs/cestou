package br.com.brunocarvalhs.howmuch.core.remoteconfig.contract

/**
 * Lê valores (chaves de API, nomes de modelo, limites, etc) controlados remotamente,
 * permitindo trocar um valor (ex: uma API key com bug) sem publicar uma nova versão do app.
 *
 * [default] é sempre retornado quando a chave ainda não foi buscada/ativada remotamente,
 * então cada chamador deve passar o mesmo valor que hoje vem do BuildConfig/.env local.
 */
interface RemoteVariableService {

    fun getString(key: String, default: String = ""): String

    fun getLong(key: String, default: Long = 0L): Long

    fun getDouble(key: String, default: Double = 0.0): Double

    fun getBoolean(key: String, default: Boolean = false): Boolean
}
