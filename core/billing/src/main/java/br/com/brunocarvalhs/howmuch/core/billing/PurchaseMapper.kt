package br.com.brunocarvalhs.howmuch.core.billing

import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import com.android.billingclient.api.Purchase

/**
 * Maps whatever `BillingClient` currently has cached locally (from `queryPurchasesAsync` or the
 * `PurchasesUpdatedListener` callback) into the domain-level [SubscriptionStatus].
 *
 * A purchase counts as Pro as soon as it reaches [Purchase.PurchaseState.PURCHASED] — including
 * before acknowledgement, since Google's 3-day auto-refund is what makes an un-acknowledged
 * purchase eventually stop being PURCHASED, not a reason to withhold entitlement while it's
 * pending acknowledgement here. [Purchase.PurchaseState.PENDING] (e.g. a cash-based payment
 * method still clearing) is deliberately not Pro yet.
 */
internal fun List<Purchase>.toSubscriptionStatus(): SubscriptionStatus =
    if (any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
        SubscriptionStatus.PRO
    } else {
        SubscriptionStatus.FREE
    }
