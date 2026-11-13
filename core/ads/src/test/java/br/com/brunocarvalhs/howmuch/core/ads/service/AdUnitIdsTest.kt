package br.com.brunocarvalhs.howmuch.core.ads.service

import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.RemoteConfigDefaults
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class AdUnitIdsTest {

    private val remoteVariables = mockk<RemoteVariableService>()

    @Test
    fun `banner unit id comes from remote config when it is set there`() {
        every {
            remoteVariables.getString(FeatureFlagKeys.ADS_BANNER_UNIT_ID, any())
        } returns "ca-app-pub-1/banner-prod"

        assertEquals("ca-app-pub-1/banner-prod", AdUnitIds(remoteVariables).banner())
    }

    @Test
    fun `interstitial unit id comes from remote config when it is set there`() {
        every {
            remoteVariables.getString(FeatureFlagKeys.ADS_INTERSTITIAL_UNIT_ID, any())
        } returns "ca-app-pub-1/inter-prod"

        assertEquals("ca-app-pub-1/inter-prod", AdUnitIds(remoteVariables).interstitial())
    }

    @Test
    fun `falls back to the safe test unit ids when remote config has no value`() {
        every {
            remoteVariables.getString(FeatureFlagKeys.ADS_BANNER_UNIT_ID, any())
        } returns RemoteConfigDefaults.ADS_BANNER_UNIT_ID_DEFAULT
        every {
            remoteVariables.getString(FeatureFlagKeys.ADS_INTERSTITIAL_UNIT_ID, any())
        } returns RemoteConfigDefaults.ADS_INTERSTITIAL_UNIT_ID_DEFAULT

        val ids = AdUnitIds(remoteVariables)

        assertEquals(RemoteConfigDefaults.ADS_BANNER_UNIT_ID_DEFAULT, ids.banner())
        assertEquals(RemoteConfigDefaults.ADS_INTERSTITIAL_UNIT_ID_DEFAULT, ids.interstitial())
    }
}
