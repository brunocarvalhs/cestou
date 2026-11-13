package br.com.brunocarvalhs.howmuch.core.ads.service

import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsPolicyTest {

    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val featureFlags = mockk<FeatureFlagService>()

    private fun policy() = AdsPolicy(subscriptionRepository, featureFlags)

    private fun givenStatusAndFlags(
        status: SubscriptionStatus,
        adsEnabled: Boolean = true,
        bannerEnabled: Boolean = true,
    ) {
        every { subscriptionRepository.observeStatus() } returns flowOf(status)
        every { featureFlags.isEnabled(FeatureFlagKeys.ADS_ENABLED, any()) } returns adsEnabled
        every { featureFlags.isEnabled(FeatureFlagKeys.ADS_BANNER_ENABLED, any()) } returns bannerEnabled
    }

    @Test
    fun `free user sees the banner when the kill switch and the banner flag are on`() = runTest {
        givenStatusAndFlags(SubscriptionStatus.FREE)

        assertTrue(policy().observeBannerAllowed().first())
    }

    @Test
    fun `pro user never sees the banner, even with every flag on`() = runTest {
        givenStatusAndFlags(SubscriptionStatus.PRO)

        assertFalse(policy().observeBannerAllowed().first())
    }

    @Test
    fun `banner is hidden when the global kill switch is off`() = runTest {
        givenStatusAndFlags(SubscriptionStatus.FREE, adsEnabled = false)

        assertFalse(policy().observeBannerAllowed().first())
    }

    @Test
    fun `banner is hidden when only the banner flag is off`() = runTest {
        givenStatusAndFlags(SubscriptionStatus.FREE, bannerEnabled = false)

        assertFalse(policy().observeBannerAllowed().first())
    }
}
