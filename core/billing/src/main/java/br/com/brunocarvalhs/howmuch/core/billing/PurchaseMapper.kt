package br.com.brunocarvalhs.howmuch.core.billing

import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import com.android.billingclient.api.Purchase

/**
 * Maps the purchases `BillingClient` reports into the domain-level [SubscriptionStatus] for
 * [accountId], the Cestou account signed in on this device (AD-010, sdd-058).
 *
 * Play's purchase cache belongs to the Google account on the device, not to the Cestou user, so a
 * purchase counts for the account it was bound to at purchase time through
 * `BillingFlowParams.Builder.setObfuscatedAccountId`. Purchases bought before that binding existed
 * carry no account and keep granting Pro to any signed-in account (grandfathered, sdd-058 Q2 (b)).
 * A purchase bound to a different account never grants Pro here. No signed-in account means Free.
 *
 * A purchase counts as Pro as soon as it reaches [Purchase.PurchaseState.PURCHASED] — including
 * before acknowledgement, since Google's 3-day auto-refund is what makes an un-acknowledged
 * purchase eventually stop being PURCHASED, not a reason to withhold entitlement while it's
 * pending acknowledgement here. [Purchase.PurchaseState.PENDING] (e.g. a cash-based payment
 * method still clearing) is deliberately not Pro yet.
 */
internal fun List<Purchase>.toSubscriptionStatus(accountId: String?): SubscriptionStatus {
    if (accountId == null) return SubscriptionStatus.FREE
    val hasGrantingPurchase = any {
        it.purchaseState == Purchase.PurchaseState.PURCHASED && it.grantsTo(accountId)
    }
    return if (hasGrantingPurchase) SubscriptionStatus.PRO else SubscriptionStatus.FREE
}

/**
 * True when this purchase entitles [accountId]: it was bound to that account, or it predates the
 * binding and so has no account at all (grandfathered).
 */
internal fun Purchase.grantsTo(accountId: String): Boolean {
    // Play reports a legacy purchase's obfuscated id as an empty string, not null (seen on device, sdd-058).
    val bound = accountIdentifiers?.obfuscatedAccountId
    if (bound.isNullOrEmpty()) return true
    return bound == accountId
}
