package br.com.brunocarvalhs.howmuch.core.billing

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import com.android.billingclient.api.AccountIdentifiers
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
import kotlinx.coroutines.flow.MutableStateFlow
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
 * The signed-in account is driven through [accountState], which backs both `authState` and
 * `currentUser` like the real `AuthService` does.
 */
@Suppress("TooManyFunctions")
class PlayBillingSubscriptionRepositoryTest {

    private companion object {
        const val AWAIT_TIMEOUT_MS = 5_000L
        const val ACCOUNT = "uid-a"
        const val OTHER_ACCOUNT = "uid-b"
    }

    private val client = mockk<BillingClient>(relaxed = true)
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val accountState = MutableStateFlow<AuthenticatedUser?>(AuthenticatedUser(id = ACCOUNT))
    private val authService = mockk<AuthService> {
        every { authState } returns accountState
        every { currentUser } answers { accountState.value }
    }
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

    /** A purchase bought while [boundTo] was signed in; `null` models a legacy, unbound purchase. */
    private fun purchase(
        state: Int,
        acked: Boolean = false,
        token: String = "tok",
        boundTo: String? = ACCOUNT
    ) = mockk<Purchase> {
        every { purchaseState } returns state
        every { isAcknowledged } returns acked
        every { purchaseToken } returns token
        every { accountIdentifiers } returns boundTo?.let { id ->
            mockk<AccountIdentifiers> { every { obfuscatedAccountId } returns id }
        }
    }

    private fun repo() = PlayBillingSubscriptionRepository(mockk<Context>(relaxed = true), authService, analytics)

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
    fun `a purchase bound to the signed-in account becomes Pro`() {
        purchasesResult = PurchasesResult(ok(), listOf(purchase(Purchase.PurchaseState.PURCHASED, acked = true)))

        awaitStatus(repo(), SubscriptionStatus.PRO)
    }

    @Test
    fun `a purchase bound to another account keeps the signed-in account Free`() {
        purchasesResult = PurchasesResult(
            ok(),
            listOf(purchase(Purchase.PurchaseState.PURCHASED, acked = true, boundTo = OTHER_ACCOUNT))
        )
        val r = repo()
        runBlocking { r.refresh() }

        assertEquals(SubscriptionStatus.FREE, runBlocking { r.observeStatus().first() })
    }

    @Test
    fun `a legacy purchase with no binding grants Pro to the signed-in account`() {
        purchasesResult = PurchasesResult(
            ok(),
            listOf(purchase(Purchase.PurchaseState.PURCHASED, acked = true, boundTo = null))
        )
        val r = repo()
        runBlocking { r.refresh() }

        assertEquals(SubscriptionStatus.PRO, runBlocking { r.observeStatus().first() })
    }

    @Test
    fun `a purchase bound to another account is reported as an account mismatch`() {
        purchasesResult = PurchasesResult(
            ok(),
            listOf(purchase(Purchase.PurchaseState.PURCHASED, acked = true, boundTo = OTHER_ACCOUNT))
        )
        runBlocking { repo().refresh() }

        verify(atLeast = 1) { analytics.trackEvent(AnalyticsEvents.ENTITLEMENT_ACCOUNT_MISMATCH, any()) }
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
        runBlocking { repo().refresh() }

        verify(atLeast = 1) { client.startConnection(any()) }
    }

    @Test
    fun `unacknowledged purchased purchase is acknowledged`() {
        purchasesResult = PurchasesResult(ok(), listOf(purchase(Purchase.PurchaseState.PURCHASED, token = "t1")))
        val r = repo()
        runBlocking { r.refresh() }

        coVerify { client.acknowledgePurchase(match { it.purchaseToken == "t1" }) }
    }

    @Test
    fun `unacknowledged purchase bound to another account is still acknowledged`() {
        // Skipping this would let Google auto-refund a purchase owned by the Play account on this device.
        purchasesResult = PurchasesResult(
            ok(),
            listOf(purchase(Purchase.PurchaseState.PURCHASED, token = "t9", boundTo = OTHER_ACCOUNT))
        )
        runBlocking { repo().refresh() }

        coVerify { client.acknowledgePurchase(match { it.purchaseToken == "t9" }) }
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
    fun `signing out drops Pro`() {
        purchasesResult = PurchasesResult(ok(), listOf(purchase(Purchase.PurchaseState.PURCHASED, acked = true)))
        val r = repo()
        awaitStatus(r, SubscriptionStatus.PRO)

        accountState.value = null

        awaitStatus(r, SubscriptionStatus.FREE)
    }

    @Test
    fun `switching to another account drops the previous account's Pro`() {
        purchasesResult = PurchasesResult(ok(), listOf(purchase(Purchase.PurchaseState.PURCHASED, acked = true)))
        val r = repo()
        awaitStatus(r, SubscriptionStatus.PRO)

        accountState.value = AuthenticatedUser(id = OTHER_ACCOUNT)
        runBlocking { r.refresh() }

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
    fun `purchase returns ITEM_UNAVAILABLE when no account is signed in`() {
        accountState.value = null

        val result = runBlocking { repo().purchase(mockk<Activity>()) }

        assertEquals(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, result.responseCode)
        verify(exactly = 0) { client.launchBillingFlow(any(), any<BillingFlowParams>()) }
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
    fun `purchase launches the billing flow bound to the signed-in account and returns its result`() {
        // Billing's param builders call TextUtils.isEmpty, which is a stub in plain JVM tests.
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
        productsResult = ProductDetailsResult(ok(), listOf(details()))
        val activity = mockk<Activity>()
        val flowResult = ok()
        // This Billing Library version exposes no getter for the obfuscated id, so the binding is
        // asserted on the builder call instead.
        val flowBuilder = mockk<BillingFlowParams.Builder>()
        val flowParams = mockk<BillingFlowParams>()
        mockkStatic(BillingFlowParams::class)
        every { BillingFlowParams.newBuilder() } returns flowBuilder
        every { flowBuilder.setProductDetailsParamsList(any()) } returns flowBuilder
        every { flowBuilder.setObfuscatedAccountId(ACCOUNT) } returns flowBuilder
        every { flowBuilder.build() } returns flowParams
        every { client.launchBillingFlow(activity, flowParams) } returns flowResult

        val result = runBlocking { repo().purchase(activity) }

        assertEquals(flowResult, result)
        verify { flowBuilder.setObfuscatedAccountId(ACCOUNT) }
    }

    @Test
    fun `refresh does not downgrade Pro status to Free when queryPurchasesAsync returns SERVICE_UNAVAILABLE`() {
        purchasesResult = PurchasesResult(ok(), listOf(purchase(Purchase.PurchaseState.PURCHASED, acked = true)))
        val r = repo()
        awaitStatus(r, SubscriptionStatus.PRO)

        purchasesResult = PurchasesResult(ok(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE), emptyList())
        runBlocking { r.refresh() }

        assertEquals(SubscriptionStatus.PRO, runBlocking { r.observeStatus().first() })
    }

    @Test
    fun `refresh does not downgrade Pro status to Free when queryPurchasesAsync throws an exception`() {
        purchasesResult = PurchasesResult(ok(), listOf(purchase(Purchase.PurchaseState.PURCHASED, acked = true)))
        val r = repo()
        awaitStatus(r, SubscriptionStatus.PRO)

        coEvery { client.queryPurchasesAsync(any<QueryPurchasesParams>()) } throws IllegalStateException("IPC dead")
        runBlocking { r.refresh() }

        assertEquals(SubscriptionStatus.PRO, runBlocking { r.observeStatus().first() })
    }

    @Test
    fun `refresh downgrades to Free when queryPurchasesAsync returns OK with empty purchase list`() {
        purchasesResult = PurchasesResult(ok(), listOf(purchase(Purchase.PurchaseState.PURCHASED, acked = true)))
        val r = repo()
        awaitStatus(r, SubscriptionStatus.PRO)

        purchasesResult = PurchasesResult(ok(), emptyList())
        runBlocking { r.refresh() }

        assertEquals(SubscriptionStatus.FREE, runBlocking { r.observeStatus().first() })
    }

    @Test
    fun `startConnection failure is handled gracefully without crashing`() {
        every { client.startConnection(any()) } answers {
            firstArg<BillingClientStateListener>().onBillingSetupFinished(
                ok(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE)
            )
        }
        ready = false
        val r = repo()

        runBlocking { r.refresh() }

        assertEquals(SubscriptionStatus.FREE, runBlocking { r.observeStatus().first() })
    }

    @Test
    fun `queryFormattedPrice returns null when connection cannot be established`() {
        ready = false
        every { client.startConnection(any()) } answers {
            firstArg<BillingClientStateListener>().onBillingSetupFinished(
                ok(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
            )
        }
        val r = repo()

        val price = runBlocking { r.queryFormattedPrice() }
        assertNull(price)
    }

    @Test
    fun `purchase returns ITEM_UNAVAILABLE when connection cannot be established`() {
        ready = false
        every { client.startConnection(any()) } answers {
            firstArg<BillingClientStateListener>().onBillingSetupFinished(
                ok(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
            )
        }
        val r = repo()

        val result = runBlocking { r.purchase(mockk<Activity>()) }
        assertEquals(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, result.responseCode)
    }
}
