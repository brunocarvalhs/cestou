package br.com.brunocarvalhs.howmuch.core.billing

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Covers [FeatureFlagKeys.BILLING_ENABLED]: while the paid business rules aren't finalized, this
 * flag defaults to off and [PlayBillingSubscriptionRepository.observeStatus] must report
 * [SubscriptionStatus.PRO] for everyone, regardless of real Play Billing state. Split out from
 * [PlayBillingSubscriptionRepositoryTest] to keep both classes under Detekt's function threshold.
 */
class PlayBillingSubscriptionRepositoryBillingToggleTest {

    private val client = mockk<BillingClient>(relaxed = true)
    private var purchasesResult = PurchasesResult(ok(), emptyList())

    @Before
    fun setUp() {
        mockkStatic(BillingClient::class)
        mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")

        val listener = slot<PurchasesUpdatedListener>()
        val builder = mockk<BillingClient.Builder>()
        every { BillingClient.newBuilder(any<Context>()) } returns builder
        every { builder.setListener(capture(listener)) } returns builder
        every { builder.enablePendingPurchases(any()) } returns builder
        every { builder.build() } returns client

        every { client.isReady } returns true
        every { client.startConnection(any()) } answers {
            firstArg<BillingClientStateListener>().onBillingSetupFinished(ok())
        }
        coEvery { client.queryPurchasesAsync(any<QueryPurchasesParams>()) } answers { purchasesResult }
    }

    @After
    fun tearDown() = unmockkAll()

    private fun ok(code: Int = BillingClient.BillingResponseCode.OK) =
        BillingResult.newBuilder().setResponseCode(code).setDebugMessage("dbg").build()

    private fun repo(billingEnabled: Boolean): PlayBillingSubscriptionRepository {
        val featureFlagService = mockk<FeatureFlagService> {
            every { isEnabled(FeatureFlagKeys.BILLING_ENABLED, default = false) } returns billingEnabled
        }
        return PlayBillingSubscriptionRepository(mockk<Context>(relaxed = true), featureFlagService)
    }

    @Test
    fun `a Free user with no purchases is reported as Pro while billing is disabled`() {
        purchasesResult = PurchasesResult(ok(), emptyList())
        val r = repo(billingEnabled = false)
        runBlocking { r.refresh() }

        assertEquals(SubscriptionStatus.PRO, runBlocking { r.observeStatus().first() })
    }

    @Test
    fun `real status still applies once billing is re-enabled`() {
        purchasesResult = PurchasesResult(ok(), emptyList())
        val r = repo(billingEnabled = true)
        runBlocking { r.refresh() }

        assertEquals(SubscriptionStatus.FREE, runBlocking { r.observeStatus().first() })
    }
}
