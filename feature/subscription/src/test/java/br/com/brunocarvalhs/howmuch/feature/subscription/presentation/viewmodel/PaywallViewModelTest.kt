package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.viewmodel

import android.app.Activity
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.billing.BillingConstants
import br.com.brunocarvalhs.howmuch.core.billing.PlanOffer
import br.com.brunocarvalhs.howmuch.core.billing.PlayBillingSubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state.PlanUi
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private val featureFlags = mockk<FeatureFlagService>()
    private val monthly = offer(BillingConstants.BASE_PLAN_MONTHLY, "R$ 9,90")
    private val annual = offer(BillingConstants.BASE_PLAN_ANNUAL, "R$ 99,90")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { billingRepository.observeStatus() } returns statusFlow
        coEvery { billingRepository.refresh() } returns Unit
        coEvery { billingRepository.queryPlans() } returns listOf(monthly, annual)
        every { featureFlags.isEnabled(any(), any()) } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun offer(basePlanId: String, price: String) = PlanOffer(
        productId = BillingConstants.PRO_MONTHLY,
        basePlanId = basePlanId,
        offerToken = "token-$basePlanId",
        formattedPrice = price,
        billingPeriod = "P1M"
    )

    private fun viewModel() = PaywallViewModel(billingRepository, analyticsTracker, featureFlags)

    @Test
    fun `with multi plans off, loads only the monthly plan and selects it`() = runTest {
        val viewModel = viewModel()

        assertEquals(listOf(PlanUi(BillingConstants.BASE_PLAN_MONTHLY, "R$ 9,90")), viewModel.uiState.value.plans)
        assertEquals(BillingConstants.BASE_PLAN_MONTHLY, viewModel.uiState.value.selectedPlanId)
        assertTrue(!viewModel.uiState.value.isLoadingPlans)
    }

    @Test
    fun `with multi plans on, lists every plan and preselects the annual one`() = runTest {
        every { featureFlags.isEnabled(FeatureFlagKeys.PAYWALL_MULTI_PLANS_ENABLED, any()) } returns true

        val viewModel = viewModel()

        assertEquals(
            listOf(
                PlanUi(BillingConstants.BASE_PLAN_MONTHLY, "R$ 9,90"),
                PlanUi(BillingConstants.BASE_PLAN_ANNUAL, "R$ 99,90")
            ),
            viewModel.uiState.value.plans
        )
        assertEquals(BillingConstants.BASE_PLAN_ANNUAL, viewModel.uiState.value.selectedPlanId)
    }

    @Test
    fun `selecting a plan updates the selection and reports it once per change`() = runTest {
        every { featureFlags.isEnabled(FeatureFlagKeys.PAYWALL_MULTI_PLANS_ENABLED, any()) } returns true
        val viewModel = viewModel()

        viewModel.intent.onPlanSelected(BillingConstants.BASE_PLAN_MONTHLY)
        viewModel.intent.onPlanSelected(BillingConstants.BASE_PLAN_MONTHLY)

        assertEquals(BillingConstants.BASE_PLAN_MONTHLY, viewModel.uiState.value.selectedPlanId)
        verify(exactly = 1) {
            analyticsTracker.trackEvent(
                AnalyticsEvents.PAYWALL_PLAN_SELECTED,
                mapOf(AnalyticsParams.PLAN_ID to BillingConstants.BASE_PLAN_MONTHLY)
            )
        }
    }

    @Test
    fun `selecting a plan that is not loaded is ignored`() = runTest {
        val viewModel = viewModel()

        viewModel.intent.onPlanSelected("unknown-plan")

        assertEquals(BillingConstants.BASE_PLAN_MONTHLY, viewModel.uiState.value.selectedPlanId)
    }

    @Test
    fun `subscribe buys the selected plan`() = runTest {
        every { featureFlags.isEnabled(FeatureFlagKeys.PAYWALL_MULTI_PLANS_ENABLED, any()) } returns true
        val activity = mockk<Activity>()
        val ok = mockk<BillingResult> { every { responseCode } returns BillingClient.BillingResponseCode.OK }
        coEvery { billingRepository.purchase(activity, any(), any()) } returns ok
        val viewModel = viewModel()
        viewModel.intent.onPlanSelected(BillingConstants.BASE_PLAN_MONTHLY)

        viewModel.intent.onSubscribeClick(activity)

        coVerify { billingRepository.purchase(activity, any(), BillingConstants.BASE_PLAN_MONTHLY) }
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
        coEvery { billingRepository.purchase(activity, any(), any()) } returns okResult
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
        coEvery { billingRepository.purchase(activity, any(), any()) } returns failureResult
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
        coEvery { billingRepository.purchase(activity, any(), any()) } returns canceledResult
        val viewModel = viewModel()

        viewModel.intent.onSubscribeClick(activity)

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `leaving without subscribing reports paywall_dismissed with the source and time spent`() {
        val viewModel = viewModel()
        viewModel.setSource("ai_gate")

        viewModel.reportDismissal()

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.PAYWALL_DISMISSED,
                match { it[AnalyticsParams.SOURCE] == "ai_gate" && (it[AnalyticsParams.DURATION_MS] as Long) >= 0 }
            )
        }
    }

    @Test
    fun `leaving after subscribing does not report paywall_dismissed`() = runTest {
        val activity = mockk<Activity>()
        val ok = mockk<BillingResult> { every { responseCode } returns BillingClient.BillingResponseCode.OK }
        coEvery { billingRepository.purchase(activity, any(), any()) } returns ok
        val viewModel = viewModel()
        viewModel.intent.onSubscribeClick(activity)

        viewModel.reportDismissal()

        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.PAYWALL_DISMISSED, any()) }
    }
}
