package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.viewmodel

import android.app.Activity
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.billing.PlayBillingSubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaywallViewModelTest {

    private val statusFlow = MutableStateFlow(SubscriptionStatus.FREE)
    private val billingRepository = mockk<PlayBillingSubscriptionRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { billingRepository.observeStatus() } returns statusFlow
        coEvery { billingRepository.refresh() } returns Unit
        coEvery { billingRepository.queryFormattedPrice() } returns "R$ 9,90"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = PaywallViewModel(billingRepository, analyticsTracker)

    @Test
    fun `loads the formatted price on init`() = runTest {
        val viewModel = viewModel()

        assertEquals("R$ 9,90", viewModel.uiState.value.formattedPrice)
        assertTrue(!viewModel.uiState.value.isLoadingPrice)
    }

    @Test
    fun `reflects the current subscription status`() = runTest {
        val viewModel = viewModel()

        statusFlow.value = SubscriptionStatus.PRO

        assertEquals(SubscriptionStatus.PRO, viewModel.uiState.value.status)
    }

    @Test
    fun `subscribe success clears any previous error`() = runTest {
        val activity = mockk<Activity>()
        val okResult = mockk<BillingResult> { every { responseCode } returns BillingClient.BillingResponseCode.OK }
        coEvery { billingRepository.purchase(activity) } returns okResult
        val viewModel = viewModel()

        viewModel.intent.onSubscribeClick(activity)

        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(!viewModel.uiState.value.isPurchasing)
    }

    @Test
    fun `subscribe failure surfaces the debug message`() = runTest {
        val activity = mockk<Activity>()
        val failureResult = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.ITEM_UNAVAILABLE
            every { debugMessage } returns "not available"
        }
        coEvery { billingRepository.purchase(activity) } returns failureResult
        val viewModel = viewModel()

        viewModel.intent.onSubscribeClick(activity)

        assertEquals("not available", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `subscribe cancellation does not surface an error`() = runTest {
        val activity = mockk<Activity>()
        val canceledResult = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.USER_CANCELED
        }
        coEvery { billingRepository.purchase(activity) } returns canceledResult
        val viewModel = viewModel()

        viewModel.intent.onSubscribeClick(activity)

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
