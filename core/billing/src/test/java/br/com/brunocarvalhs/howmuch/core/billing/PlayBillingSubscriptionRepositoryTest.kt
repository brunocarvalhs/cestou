package br.com.brunocarvalhs.howmuch.core.billing

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * The repository builds its own `BillingClient` and runs on `Dispatchers.IO`, so the tests mock
 * the static builder and billing-ktx extensions and wait on the status flow with a real timeout.
 */
@Suppress("TooManyFunctions")
class PlayBillingSubscriptionRepositoryTest {

    private companion object {
        const val AWAIT_TIMEOUT_MS = 5_000L
    }

    private val client = mockk<BillingClient>(relaxed = true)
    private lateinit var purchasesListener: PurchasesUpdatedListener
    private var ready = true
    private var purchasesResult = PurchasesResult(ok(), emptyList())
    private var productsResult = ProductDetailsResult(ok(), emptyList())
    private var ackResult = ok()

    @Before
    fun setUp() {
        mockkStatic(BillingClient::class)
        mockkStatic("com.android.billingclient.api.BillingClientKotlinKt")

        val listener = slot<PurchasesUpdatedListener>()
        val builder = mockk<BillingClient.Builder>()
        every { BillingClient.newBuilder(any<Context>()) } returns builder
        every { builder.setListener(capture(listener)) } returns builder
        every { builder.enablePendingPurchases(any()) } returns builder
        every { builder.build() } answers {
            purchasesListener = listener.captured
            client
        }

        every { client.isReady } answers { ready }
        every { client.startConnection(any()) } answers {
            firstArg<BillingClientStateListener>().onBillingSetupFinished(ok())
        }
        coEvery { client.queryPurchasesAsync(any<QueryPurchasesParams>()) } answers { purchasesResult }
        coEvery { client.queryProductDetails(any<QueryProductDetailsParams>()) } answers { productsResult }
        coEvery { client.acknowledgePurchase(any<AcknowledgePurchaseParams>()) } answers { ackResult }
    }

    @After
    fun tearDown() = unmockkAll()

    private fun ok(code: Int = BillingClient.BillingResponseCode.OK) =
        BillingResult.newBuilder().setResponseCode(code).setDebugMessage("dbg").build()

    private fun purchase(state: Int, acked: Boolean = false, token: String = "tok") =
        mockk<Purchase> {
            every { purchaseState } returns state
            every { isAcknowledged } returns acked
            every { purchaseToken } returns token
        }

    private fun repo() = PlayBillingSubscriptionRepository(mockk<Context>(relaxed = true))

    private fun details(price: String = "R$ 9,90", offerToken: String? = "offer"): ProductDetails {
        val phase = mockk<ProductDetails.PricingPhase> { every { formattedPrice } returns price }
        val offer = mockk<ProductDetails.SubscriptionOfferDetails> {
            every { this@mockk.offerToken } returns offerToken.orEmpty()
            every { pricingPhases } returns mockk {
                every { pricingPhaseList } returns listOf(phase)
            }
        }
        return mockk(relaxed = true) {
            every { subscriptionOfferDetails } returns if (offerToken == null) null else listOf(offer)
        }
    }

    private fun awaitStatus(r: PlayBillingSubscriptionRepository, expected: SubscriptionStatus) =
        runBlocking { withTimeout(AWAIT_TIMEOUT_MS) { r.observeStatus().first { it == expected } } }

    @Test
    fun `init refreshes and a purchased subscription becomes Pro`() {
        purchasesResult = PurchasesResult(ok(), listOf(purchase(Purchase.PurchaseState.PURCHASED, acked = true)))

        awaitStatus(repo(), SubscriptionStatus.PRO)
    }

    @Test
    fun `status stays Free when there are no purchases`() {
        val r = repo()
        runBlocking { r.refresh() }

        assertEquals(SubscriptionStatus.FREE, runBlocking { r.observeStatus().first() })
    }

    @Test
    fun `refresh reconnects when the client is not ready`() {
        ready = false
        val r = repo()
        runBlocking { r.refresh() }

        verify(atLeast = 2) { client.startConnection(any()) }
    }

    @Test
    fun `unacknowledged purchased purchase is acknowledged`() {
        purchasesResult = PurchasesResult(ok(), listOf(purchase(Purchase.PurchaseState.PURCHASED, token = "t1")))
        val r = repo()
        runBlocking { r.refresh() }

        coVerify { client.acknowledgePurchase(match { it.purchaseToken == "t1" }) }
    }

    @Test
    fun `acknowledged and pending purchases are not acknowledged`() {
        purchasesResult = PurchasesResult(
            ok(),
            listOf(
                purchase(Purchase.PurchaseState.PURCHASED, acked = true),
                purchase(Purchase.PurchaseState.PENDING)
            )
        )
        runBlocking { repo().refresh() }

        coVerify(exactly = 0) { client.acknowledgePurchase(any<AcknowledgePurchaseParams>()) }
    }

    @Test
    fun `failed acknowledge is tolerated and status is still Pro`() {
        ackResult = ok(BillingClient.BillingResponseCode.ERROR)
        purchasesResult = PurchasesResult(ok(), listOf(purchase(Purchase.PurchaseState.PURCHASED)))
        val r = repo()
        runBlocking { r.refresh() }

        assertEquals(SubscriptionStatus.PRO, runBlocking { r.observeStatus().first() })
    }

    @Test
    fun `purchases updated callback with OK moves status to Pro`() {
        val r = repo()
        runBlocking { r.refresh() }

        purchasesListener.onPurchasesUpdated(
            ok(),
            listOf(purchase(Purchase.PurchaseState.PURCHASED, acked = true))
        )

        awaitStatus(r, SubscriptionStatus.PRO)
    }

    @Test
    fun `purchases updated callback with an error or null purchases keeps status`() {
        val r = repo()
        runBlocking { r.refresh() }

        purchasesListener.onPurchasesUpdated(ok(BillingClient.BillingResponseCode.USER_CANCELED), null)
        purchasesListener.onPurchasesUpdated(ok(), null)

        assertEquals(SubscriptionStatus.FREE, runBlocking { r.observeStatus().first() })
    }

    @Test
    fun `queryFormattedPrice returns the first offer price`() {
        productsResult = ProductDetailsResult(ok(), listOf(details("R$ 9,90")))

        assertEquals("R$ 9,90", runBlocking { repo().queryFormattedPrice() })
    }

    @Test
    fun `queryFormattedPrice is null when product or offer is missing`() {
        val r = repo()
        assertNull(runBlocking { r.queryFormattedPrice() })

        productsResult = ProductDetailsResult(ok(), listOf(details(offerToken = null)))
        assertNull(runBlocking { r.queryFormattedPrice() })
    }

    @Test
    fun `purchase returns ITEM_UNAVAILABLE when the product is not found`() {
        val result = runBlocking { repo().purchase(mockk<Activity>()) }

        assertEquals(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, result.responseCode)
    }

    @Test
    fun `purchase returns ITEM_UNAVAILABLE when the product has no offer`() {
        productsResult = ProductDetailsResult(ok(), listOf(details(offerToken = null)))

        val result = runBlocking { repo().purchase(mockk<Activity>()) }

        assertEquals(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, result.responseCode)
    }

    @Test
    fun `purchase launches the billing flow and returns its result`() {
        // Billing's param builders call TextUtils.isEmpty, which is a stub in plain JVM tests.
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
        productsResult = ProductDetailsResult(ok(), listOf(details()))
        val activity = mockk<Activity>()
        val flowResult = ok()
        every { client.launchBillingFlow(activity, any<BillingFlowParams>()) } returns flowResult

        val result = runBlocking { repo().purchase(activity) }

        assertEquals(flowResult, result)
    }
}
