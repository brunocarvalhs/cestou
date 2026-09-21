package br.com.brunocarvalhs.howmuch.core.remoteconfig.model

import kotlinx.serialization.Serializable

/**
 * Estrutura opcional de uma feature flag no Remote Config. Além do liga/desliga simples,
 * permite restringir em quais versões do app ela fica ativa — útil para desligar uma
 * funcionalidade só nas versões onde um bug foi identificado, sem depender de publicar
 * uma nova versão do app.
 *
 * Valor remoto esperado (parâmetro do tipo JSON no console do Firebase), ex:
 * `{ "enabled": true, "minVersion": "1.2.0", "maxVersion": "1.4.0", "disabledVersions": ["1.3.1"] }`
 *
 * Flags que não precisam de escopo por versão continuam podendo usar um valor
 * `"true"`/`"false"` puro; [br.com.brunocarvalhs.howmuch.core.remoteconfig.service.FirebaseRemoteConfigService]
 * trata os dois formatos.
 */
@Serializable
data class FeatureFlagConfig(
    val enabled: Boolean = false,
    val minVersion: String? = null,
    val maxVersion: String? = null,
    val disabledVersions: List<String> = emptyList()
)
