package br.com.brunocarvalhs.data.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import br.com.brunocarvalhs.data.model.PlanSubscriptionModel
import br.com.brunocarvalhs.domain.entities.PlanSubscription
import br.com.brunocarvalhs.domain.services.SubscriptionService
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import androidx.core.net.toUri

class GooglePlaySubscriptionService(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : SubscriptionService {

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (purchases != null) {
                        for (purchase in purchases) {
                            handlePurchase(purchase)
                        }
                    } else {
                        println("Purchase update OK, but purchase list is null.")
                    }
                }

                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    println("User cancelled the purchase flow.")
                }

                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    println("User already owns this item.")
                    scope.launch { isUserPremium() }
                }

                else -> {
                    println("Purchase error: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
                }
            }
        }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                scope.launch {
                    acknowledgePurchase(purchase)
                }
            }
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        suspendCancellableCoroutine { continuation ->
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (continuation.isActive) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        println("Purchase successfully acknowledged.")
                    }
                    continuation.resume(Unit)
                }
            }
        }
    }

    val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    private var productDetails: ProductDetails? = null

    init {
        connectToGooglePlayBilling()
    }

    private fun connectToGooglePlayBilling() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryProductDetails()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                connectToGooglePlayBilling()
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_subscription")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                this@GooglePlaySubscriptionService.productDetails =
                    productDetailsList.productDetailsList.firstOrNull()
            }
        }
    }

    override suspend fun isUserPremium(): Boolean = suspendCancellableCoroutine { continuation ->
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            val isPremium =
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.isAcknowledged }
                } else {
                    false
                }
            if (continuation.isActive) {
                continuation.resume(isPremium)
            }
        }
    }

    override suspend fun launchPremiumPurchase(activity: Activity): Result<Unit> =
        withContext(Dispatchers.Main) {
            val details = productDetails ?: run {
                println("Product details not loaded yet. Cannot launch purchase flow.")
                return@withContext Result.failure(Exception("Product details not loaded yet."))
            }

            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                println("No valid subscription offer found for the product.")
                return@withContext Result.failure(Exception("No valid subscription offer found for the product."))
            }

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
            Result.success(Unit)
        }

    override suspend fun listPlans(): List<PlanSubscription> =
        suspendCancellableCoroutine { continuation ->
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("premium_subscription")
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val plans = productDetailsList.productDetailsList.map {

                        val basePlan = productDetails?.subscriptionOfferDetails?.firstOrNull()
                        val pricingPhase = basePlan?.pricingPhases?.pricingPhaseList?.firstOrNull()

                        if (basePlan == null || pricingPhase == null) {
                            null // mapNotNull irá remover este item da lista final.
                        } else {
                            PlanSubscriptionModel(
                                id = it.productId,
                                name = it.name,
                                price = pricingPhase.formattedPrice,
                                offerToken = basePlan.offerToken,
                                features = basePlan.pricingPhases.pricingPhaseList.map { phase ->
                                    phase.formattedPrice
                                },
                                isRecommended = it.productId == "premium_subscription",
                                renewsAt = pricingPhase.billingPeriod
                            )
                        }
                    }
                    continuation.resume(plans as List<PlanSubscription>)
                } else {
                    println("Error fetching plans: ${billingResult.debugMessage}")
                    continuation.resume(emptyList())
                }
            }
        }

    override suspend fun cancelSubscription(activity: Activity): Result<Unit> {
        return try {
            // Primeiro, você precisa saber qual assinatura o usuário possui.
            // Vamos assumir que temos o ID do produto da assinatura ativa.
            val activeSubscriptionId = "premium_subscription"

            val url =
                "https://play.google.com/store/account/subscriptions?sku=$activeSubscriptionId&package=${activity.packageName}"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = url.toUri()
            }

            activity.startActivity(intent)

            Result.success(Unit)
        } catch (e: Exception) {
            println("Failed to launch subscription management page: ${e.message}")
            Result.failure(e)
        }
    }
}
