package br.com.brunocarvalhs.howmuch.core.ads.service

import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Decide se o banner pode aparecer. Só o plano FREE vê anúncios, e só com o kill switch global
 * e a flag do banner ligados. Consentimento de privacidade é tratado à parte, no momento de
 * carregar o anúncio ([ConsentGateway]).
 */
internal class AdsPolicy @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val featureFlags: FeatureFlagService,
) {

    fun observeBannerAllowed(): Flow<Boolean> =
        subscriptionRepository.observeStatus()
            .map { status ->
                status == SubscriptionStatus.FREE &&
                    featureFlags.isEnabled(FeatureFlagKeys.ADS_ENABLED, default = false) &&
                    featureFlags.isEnabled(FeatureFlagKeys.ADS_BANNER_ENABLED, default = false)
            }
            .distinctUntilChanged()
}
