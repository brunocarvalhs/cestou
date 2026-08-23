package br.com.brunocarvalhs.howmuch.core.remoteconfig.service

import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteConfigSyncService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.exception.RemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação única para as três interfaces de Remote Config, já que no Firebase
 * flags, variáveis e o ciclo de fetch/activate compartilham a mesma instância.
 *
 * Quando uma chave ainda não foi buscada/ativada remotamente (fonte estática do SDK),
 * o [default] passado pelo chamador é usado no lugar do valor do Firebase.
 */
@Singleton
class FirebaseRemoteConfigService @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val crashReporter: CrashReporter
) : FeatureFlagService, RemoteVariableService, RemoteConfigSyncService {

    override fun isEnabled(key: String, default: Boolean): Boolean = getBoolean(key, default)

    override fun getString(key: String, default: String): String {
        val value = remoteConfig.getValue(key)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) default else value.asString()
    }

    override fun getLong(key: String, default: Long): Long {
        val value = remoteConfig.getValue(key)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) default else value.asLong()
    }

    override fun getDouble(key: String, default: Double): Double {
        val value = remoteConfig.getValue(key)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) default else value.asDouble()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        val value = remoteConfig.getValue(key)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) default else value.asBoolean()
    }

    override suspend fun refresh(): Boolean = try {
        remoteConfig.fetchAndActivate().await()
    } catch (e: Exception) {
        crashReporter.recordException(RemoteConfigException("Falha ao atualizar Remote Config", e))
        false
    }
}
