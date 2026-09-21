package br.com.brunocarvalhs.howmuch.core.remoteconfig.contract

/**
 * Dispara uma busca manual de Remote Config (além do fetch automático de inicialização),
 * útil por exemplo após o app voltar do background.
 */
interface RemoteConfigSyncService {

    suspend fun refresh(): Boolean
}
