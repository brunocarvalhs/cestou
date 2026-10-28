package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.billing.BillingConstants
import br.com.brunocarvalhs.howmuch.core.billing.PlayBillingSubscriptionRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ManageSubscriptionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val billingRepository = mockk<PlayBillingSubscriptionRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private fun viewModel(): ManageSubscriptionViewModel {
        coEvery { billingRepository.queryFormattedPrice() } returns "R$ 9,90"
        return ManageSubscriptionViewModel(context, billingRepository, analyticsTracker)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tracks screen_view on init`() {
        viewModel()

        verify { analyticsTracker.trackScreenView("manage_subscription", "ManageSubscriptionViewModel") }
    }

    @Test
    fun `loads the formatted price on init`() = runTest {
        val vm = viewModel()

        assertEquals("R$ 9,90", vm.uiState.value.formattedPrice)
        assertTrue(!vm.uiState.value.isLoadingPrice)
    }

    @Test
    fun `onManageOnPlayStore opens the Play Store subscriptions page for the Pro product and tracks the event`() {
        val vm = viewModel()

        vm.intent.onManageOnPlayStore()

        val started = shadowOf(context as Application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        val url = started.dataString.orEmpty()
        assertTrue(url.startsWith("https://play.google.com/store/account/subscriptions"))
        assertTrue(url.contains("sku=${BillingConstants.PRO_MONTHLY}"))
        assertTrue(url.contains("package=${context.packageName}"))
        verify { analyticsTracker.trackEvent(AnalyticsEvents.MANAGE_SUBSCRIPTION_PLAY_STORE_OPENED) }
    }
}
