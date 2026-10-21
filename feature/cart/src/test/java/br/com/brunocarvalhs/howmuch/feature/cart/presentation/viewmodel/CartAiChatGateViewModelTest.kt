package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class CartAiChatGateViewModelTest {

    private fun featureFlagService(aiChatEnabled: Boolean) = mockk<FeatureFlagService> {
        every { isEnabled(FeatureFlagKeys.AI_CHAT_ENABLED, default = false) } returns aiChatEnabled
    }

    @Test
    fun `FAB is hidden when the flag is off`() {
        val viewModel = CartAiChatGateViewModel(featureFlagService(aiChatEnabled = false))

        assertEquals(false, viewModel.showAiChatFab)
    }

    @Test
    fun `FAB shows when the flag is on`() {
        val viewModel = CartAiChatGateViewModel(featureFlagService(aiChatEnabled = true))

        assertEquals(true, viewModel.showAiChatFab)
    }
}
