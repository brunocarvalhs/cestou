package br.com.brunocarvalhs.howmuch.core.billing

import android.app.Activity
import android.app.Application
import android.content.Context
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * [SubscriptionRepository] implementation backed by Play Billing (AD-010). Owns the
 * `BillingClient` connection for the whole app lifetime, keeps [observeStatus] in sync with
 * whatever Play reports (on connect and on every [PurchasesUpdatedListener] callback), and
 * acknowledges new purchases inline — required within 3 days or Google auto-refunds them, so
 * this is not optional polish.
 *
 * [purchase] is the Activity-bound entry point: it looks up the real, current price via
 * `queryProductDetails` (never a hardcoded value) and launches Play's own purchase UI.
 *
 * There is no explicit "restore purchases" action: [refresh], called from [purchase]'s caller
 * on every app resume, re-runs `queryPurchasesAsync` against Play's local cache, which *is*
 * restore on Android.
 */
@Singleton
class PlayBillingSubscriptionRepository @Inject constructor(
    @ApplicationContext context: Context
) : SubscriptionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val statusCache = BillingStatusCache(context)
    private val connectionMutex = Mutex()

    private val _status = MutableStateFlow(statusCache.load())

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { handlePurchases(purchases) }
        } else {
            Timber.tag(TAG).w(
                "onPurchasesUpdated: responseCode=%d debugMessage=%s",
                result.responseCode,
                result.debugMessage
            )
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            // Billing Library 7+ requires this builder to explicitly declare which purchase
            // types support the PENDING state, even though this app only sells subscriptions
            // (whose pending-purchase support is implicit/always on) — build() throws
            // IllegalArgumentException otherwise. No one-time products are actually sold.
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        scope.launch {
            ensureConnected()
            refresh()
        }
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(
            BillingLifecycleObserver {
                scope.launch { refresh() }
            }
        )
    }

    override fun observeStatus(): Flow<SubscriptionStatus> = _status.asStateFlow()

    /**
     * Re-reads Play's local purchase cache and updates [observeStatus] accordingly. Call this
     * on every app resume — it is the app's entire "restore purchases" story (AD-010).
     */
    suspend fun refresh() {
        if (!ensureConnected()) {
            Timber.tag(TAG).w("refresh skipped: could not connect to BillingClient")
            return
        }
        val result = try {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "queryPurchasesAsync threw an exception")
            return
        }

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            handlePurchases(result.purchasesList)
        } else {
            Timber.tag(TAG).w(
                "queryPurchasesAsync failed: responseCode=%d debugMessage=%s (retaining current status: %s)",
                result.billingResult.responseCode,
                result.billingResult.debugMessage,
                _status.value
            )
        }
    }

    /**
     * The price to show on the Paywall before purchase, exactly as Play will charge it —
     * resolved via `queryProductDetails`, never hardcoded (AD-010). `null` when the product
     * isn't configured in the Play Console yet or has no subscription offer.
     */
    suspend fun queryFormattedPrice(productId: String = BillingConstants.PRO_MONTHLY): String? {
        if (!ensureConnected()) return null
        return try {
            val offer = queryProductDetails(productId)?.subscriptionOfferDetails?.firstOrNull()
            offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "queryFormattedPrice failed for %s", productId)
            null
        }
    }

    /**
     * The Activity-bound purchase entry point. Looks up [productId]'s current price/offer via
     * `queryProductDetails` and launches Play's purchase UI on top of [activity]. The result of
     * the purchase itself arrives asynchronously through [purchasesUpdatedListener], not through
     * this function's return value.
     */
    suspend fun purchase(
        activity: Activity,
        productId: String = BillingConstants.PRO_MONTHLY
    ): BillingResult {
        if (!ensureConnected()) {
            return unavailableResult("BillingClient connection unavailable")
        }

        return try {
            val productDetails = queryProductDetails(productId)
                ?: return unavailableResult("Product $productId not found in Play Console")

            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                ?: return unavailableResult("Product $productId has no subscription offer")

            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .build()

            billingClient.launchBillingFlow(activity, params)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "launchBillingFlow failed for %s", productId)
            unavailableResult("Failed to launch billing flow: ${e.message}")
        }
    }

    private suspend fun queryProductDetails(productId: String) = try {
        billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()
        ).productDetailsList?.firstOrNull()
    } catch (e: Exception) {
        Timber.tag(TAG).w(e, "queryProductDetails failed for %s", productId)
        null
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        val newStatus = purchases.toSubscriptionStatus()
        _status.value = newStatus
        statusCache.save(newStatus)

        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { acknowledge(it) }
    }

    private suspend fun acknowledge(purchase: Purchase) {
        try {
            val result = billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            )
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Timber.tag(TAG).w(
                    "acknowledgePurchase failed: responseCode=%d debugMessage=%s",
                    result.responseCode,
                    result.debugMessage
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "acknowledgePurchase threw exception for purchase %s", purchase.purchaseToken)
        }
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return connectionMutex.withLock {
            if (billingClient.isReady) return@withLock true
            connectWithRetry()
        }
    }

    private suspend fun connectWithRetry(): Boolean {
        var backoff = INITIAL_BACKOFF_MS
        for (attempt in 1..MAX_CONNECT_RETRIES) {
            val connected = connectSingleAttempt()
            if (connected) return true
            if (attempt < MAX_CONNECT_RETRIES) {
                delay(backoff)
                backoff *= 2
            }
        }
        Timber.tag(TAG).w("BillingClient failed to connect after %d attempts", MAX_CONNECT_RETRIES)
        return false
    }

    private suspend fun connectSingleAttempt(): Boolean {
        return try {
            withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    try {
                        billingClient.startConnection(object : BillingClientStateListener {
                            override fun onBillingSetupFinished(billingResult: BillingResult) {
                                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                    if (continuation.isActive) continuation.resume(true)
                                } else {
                                    Timber.tag(TAG).w(
                                        "Billing setup failed: responseCode=%d debugMessage=%s",
                                        billingResult.responseCode,
                                        billingResult.debugMessage
                                    )
                                    if (continuation.isActive) continuation.resume(false)
                                }
                            }

                            override fun onBillingServiceDisconnected() {
                                Timber.tag(TAG).w("Billing service disconnected; will reconnect on next use")
                            }
                        })
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "Exception while starting BillingClient connection")
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
            } ?: run {
                Timber.tag(TAG).w("BillingClient connection timed out after %d ms", CONNECT_TIMEOUT_MS)
                false
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Unexpected error in connectSingleAttempt")
            false
        }
    }

    private fun unavailableResult(message: String): BillingResult {
        Timber.tag(TAG).w(message)
        return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
            .setDebugMessage(message)
            .build()
    }

    companion object {
        private const val TAG = "PlayBillingSubscriptionRepository"
        private const val CONNECT_TIMEOUT_MS = 10_000L
        private const val MAX_CONNECT_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 500L
    }
}
