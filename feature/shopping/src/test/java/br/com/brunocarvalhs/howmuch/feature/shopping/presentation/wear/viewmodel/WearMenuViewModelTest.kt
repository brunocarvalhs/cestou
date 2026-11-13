package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class WearMenuViewModelTest {

    private val analytics = mockk<AnalyticsTracker>(relaxed = true)

    private fun flags(aiEnabled: Boolean): FeatureFlagService = mockk {
        every { isEnabled(FeatureFlagKeys.WEAR_AI_QUICK_ENTRY, default = false) } returns aiEnabled
    }

    @Test
    fun `shows the ai entry when the flag is on`() {
        val vm = WearMenuViewModel(flags(aiEnabled = true), analytics)

        assertEquals(true, vm.uiState.value.showAi)
    }

    @Test
    fun `hides the ai entry when the flag is off`() {
        val vm = WearMenuViewModel(flags(aiEnabled = false), analytics)

        assertEquals(false, vm.uiState.value.showAi)
    }

    @Test
    fun `tracks the menu open`() {
        WearMenuViewModel(flags(aiEnabled = false), analytics)

        verify { analytics.trackEvent(AnalyticsEvents.WEAR_MENU_OPEN, any()) }
    }
}
