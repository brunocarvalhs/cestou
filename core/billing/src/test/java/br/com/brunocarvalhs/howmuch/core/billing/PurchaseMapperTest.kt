package br.com.brunocarvalhs.howmuch.core.billing

import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import com.android.billingclient.api.AccountIdentifiers
import com.android.billingclient.api.Purchase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PurchaseMapperTest {

    private val account = "uid-a"

    /** A purchase bought while [boundTo] was signed in; `null` models a legacy, unbound purchase. */
    private fun purchaseWithState(state: Int, boundTo: String? = account): Purchase = mockk {
        every { purchaseState } returns state
        every { accountIdentifiers } returns boundTo?.let { id ->
            mockk<AccountIdentifiers> { every { obfuscatedAccountId } returns id }
        }
    }

    @Test
    fun `no purchases maps to Free`() {
        assertEquals(SubscriptionStatus.FREE, emptyList<Purchase>().toSubscriptionStatus(account))
    }

    @Test
    fun `a PURCHASED purchase bound to the current account maps to Pro`() {
        val purchases = listOf(purchaseWithState(Purchase.PurchaseState.PURCHASED))

        assertEquals(SubscriptionStatus.PRO, purchases.toSubscriptionStatus(account))
    }

    @Test
    fun `a PURCHASED purchase bound to another account maps to Free`() {
        val purchases = listOf(purchaseWithState(Purchase.PurchaseState.PURCHASED, boundTo = "uid-b"))

        assertEquals(SubscriptionStatus.FREE, purchases.toSubscriptionStatus(account))
    }

    @Test
    fun `a legacy PURCHASED purchase with no account binding grandfathers Pro to any signed-in account`() {
        val purchases = listOf(purchaseWithState(Purchase.PurchaseState.PURCHASED, boundTo = null))

        assertEquals(SubscriptionStatus.PRO, purchases.toSubscriptionStatus(account))
        assertEquals(SubscriptionStatus.PRO, purchases.toSubscriptionStatus("uid-b"))
    }

    @Test
    fun `a legacy PURCHASED purchase reported with an empty obfuscated id grandfathers Pro`() {
        val purchases = listOf(purchaseWithState(Purchase.PurchaseState.PURCHASED, boundTo = ""))

        assertEquals(SubscriptionStatus.PRO, purchases.toSubscriptionStatus(account))
    }

    @Test
    fun `no signed-in account maps to Free even with a bound PURCHASED purchase`() {
        val purchases = listOf(purchaseWithState(Purchase.PurchaseState.PURCHASED))

        assertEquals(SubscriptionStatus.FREE, purchases.toSubscriptionStatus(null))
    }

    @Test
    fun `only PENDING purchases still map to Free`() {
        val purchases = listOf(purchaseWithState(Purchase.PurchaseState.PENDING))

        assertEquals(SubscriptionStatus.FREE, purchases.toSubscriptionStatus(account))
    }

    @Test
    fun `a PENDING purchase alongside a bound PURCHASED one maps to Pro`() {
        val purchases = listOf(
            purchaseWithState(Purchase.PurchaseState.PENDING),
            purchaseWithState(Purchase.PurchaseState.PURCHASED)
        )

        assertEquals(SubscriptionStatus.PRO, purchases.toSubscriptionStatus(account))
    }
}
