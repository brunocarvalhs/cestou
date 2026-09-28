package br.com.brunocarvalhs.howmuch.core.billing

import android.app.Activity
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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

    private val _status = MutableStateFlow(SubscriptionStatus.FREE)

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
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().build())
        .build()

    init {
        scope.launch {
            connect()
            refresh()
        }
    }

    override fun observeStatus(): Flow<SubscriptionStatus> = _status.asStateFlow()

    /**
     * Re-reads Play's local purchase cache and updates [observeStatus] accordingly. Call this
     * on every app resume — it is the app's entire "restore purchases" story (AD-010).
     */
    suspend fun refresh() {
        ensureConnected()
        val result = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        handlePurchases(result.purchasesList)
    }

    /**
     * The price to show on the Paywall before purchase, exactly as Play will charge it —
     * resolved via `queryProductDetails`, never hardcoded (AD-010). `null` when the product
     * isn't configured in the Play Console yet or has no subscription offer.
     */
    suspend fun queryFormattedPrice(productId: String = BillingConstants.PRO_MONTHLY): String? {
        ensureConnected()
        val offer = queryProductDetails(productId)?.subscriptionOfferDetails?.firstOrNull()
        return offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
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
        ensureConnected()

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

        return billingClient.launchBillingFlow(activity, params)
    }

    private suspend fun queryProductDetails(productId: String) =
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

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        _status.value = purchases.toSubscriptionStatus()

        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { acknowledge(it) }
    }

    private suspend fun acknowledge(purchase: Purchase) {
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
    }

    // billing-ktx has no suspend `startConnection`; only the callback-based one exists.
    private suspend fun connect() = suspendCancellableCoroutine { continuation ->
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (continuation.isActive) continuation.resume(Unit)
            }

            override fun onBillingServiceDisconnected() {
                Timber.tag(TAG).w("Billing service disconnected; will reconnect on next use")
            }
        })
    }

    private suspend fun ensureConnected() {
        if (!billingClient.isReady) connect()
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
    }
}
