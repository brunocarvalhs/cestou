package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Intersticial de IA (sdd-059): dispara só quando o FREE consome a mensagem gratuita agora. */
class ChatAiGateInterstitialTest {

    private val statusFlow = MutableStateFlow(SubscriptionStatus.FREE)
    private val usedFlow = MutableStateFlow(false)
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val aiTrialRepository = mockk<AiTrialRepository>()
    private val featureFlagService = mockk<FeatureFlagService>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { subscriptionRepository.observeStatus() } returns statusFlow
        every { aiTrialRepository.hasUsedFreeMessage() } returns usedFlow
        every { featureFlagService.isEnabled(any(), any()) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ChatAiGateViewModel(subscriptionRepository, aiTrialRepository, featureFlagService)

    @Test
    fun `free user who consumes the first message gets one interstitial request`() = runTest {
        viewModel().aiInterstitialRequests.test {
            usedFlow.value = true

            awaitItem()
            expectNoEvents()
        }
    }

    @Test
    fun `trial already used when the chat opens does not request an interstitial`() = runTest {
        usedFlow.value = true

        viewModel().aiInterstitialRequests.test {
            expectNoEvents()
        }
    }

    @Test
    fun `pro user never gets an interstitial request`() = runTest {
        statusFlow.value = SubscriptionStatus.PRO

        viewModel().aiInterstitialRequests.test {
            usedFlow.value = true

            expectNoEvents()
        }
    }

    @Test
    fun `no interstitial request when the global ads flag is off`() = runTest {
        every { featureFlagService.isEnabled(FeatureFlagKeys.ADS_ENABLED, any()) } returns false

        viewModel().aiInterstitialRequests.test {
            usedFlow.value = true

            expectNoEvents()
        }
    }

    @Test
    fun `no interstitial request when the ai interstitial flag is off`() = runTest {
        every {
            featureFlagService.isEnabled(FeatureFlagKeys.ADS_AI_INTERSTITIAL_ENABLED, any())
        } returns false

        viewModel().aiInterstitialRequests.test {
            usedFlow.value = true

            expectNoEvents()
        }
    }
}
