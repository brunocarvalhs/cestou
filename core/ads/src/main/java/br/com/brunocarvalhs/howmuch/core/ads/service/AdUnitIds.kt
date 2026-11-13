package br.com.brunocarvalhs.howmuch.core.ads.service

import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.RemoteConfigDefaults
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IDs das unidades de anúncio, lidos do Remote Config. Trocar uma unidade (ex.: de teste para
 * produção) é ação no console, sem republicar o app. Antes do primeiro fetch, vale o default
 * de teste do AdMob, que é seguro.
 */
@Singleton
internal class AdUnitIds @Inject constructor(
    private val remoteVariables: RemoteVariableService,
) {

    fun banner(): String = remoteVariables.getString(
        FeatureFlagKeys.ADS_BANNER_UNIT_ID,
        RemoteConfigDefaults.ADS_BANNER_UNIT_ID_DEFAULT,
    )

    fun interstitial(): String = remoteVariables.getString(
        FeatureFlagKeys.ADS_INTERSTITIAL_UNIT_ID,
        RemoteConfigDefaults.ADS_INTERSTITIAL_UNIT_ID_DEFAULT,
    )
}
