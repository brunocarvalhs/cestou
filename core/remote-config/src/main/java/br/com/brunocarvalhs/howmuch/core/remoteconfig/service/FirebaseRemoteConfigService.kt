package br.com.brunocarvalhs.howmuch.core.remoteconfig.service

import br.com.brunocarvalhs.howmuch.core.common.contract.AppVersionProvider
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.common.util.isAtLeastVersion
import br.com.brunocarvalhs.howmuch.core.common.util.isAtMostVersion
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteConfigSyncService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.exception.RemoteConfigException
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
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
    private val crashReporter: CrashReporter,
    private val versionProvider: AppVersionProvider
) : FeatureFlagService, RemoteVariableService, RemoteConfigSyncService {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Aceita tanto um valor JSON ([FeatureFlagConfig], com escopo de versão) quanto um
     * boolean puro ("true"/"false"), para compatibilidade com flags simples já publicadas.
     */
    override fun isEnabled(key: String, default: Boolean): Boolean {
        val value = remoteConfig.getValue(key)
        if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return default

        val raw = value.asString()
        val config = parseFlagConfig(raw) ?: return raw.toBooleanStrictOrNull() ?: default

        return config.isEnabledForVersion(versionProvider.versionName())
    }

    private fun FeatureFlagConfig.isEnabledForVersion(currentVersion: String): Boolean {
        if (!enabled) return false
        if (currentVersion in disabledVersions) return false
        minVersion?.let { if (!currentVersion.isAtLeastVersion(it)) return false }
        maxVersion?.let { if (!currentVersion.isAtMostVersion(it)) return false }
        return true
    }

    private fun parseFlagConfig(raw: String): FeatureFlagConfig? {
        if (raw.isBlank() || !raw.trimStart().startsWith("{")) return null
        return try {
            json.decodeFromString<FeatureFlagConfig>(raw)
        } catch (e: Exception) {
            crashReporter.recordException(RemoteConfigException("Falha ao ler configuração da flag: $raw", e))
            null
        }
    }

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
