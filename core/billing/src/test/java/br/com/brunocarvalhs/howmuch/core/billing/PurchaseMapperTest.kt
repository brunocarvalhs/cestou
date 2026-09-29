package br.com.brunocarvalhs.howmuch.core.billing

import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import com.android.billingclient.api.Purchase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PurchaseMapperTest {

    private fun purchaseWithState(state: Int): Purchase = mockk {
        every { purchaseState } returns state
    }

    @Test
    fun `no purchases maps to Free`() {
        assertEquals(SubscriptionStatus.FREE, emptyList<Purchase>().toSubscriptionStatus())
    }

    @Test
    fun `a PURCHASED purchase maps to Pro`() {
        val purchases = listOf(purchaseWithState(Purchase.PurchaseState.PURCHASED))

        assertEquals(SubscriptionStatus.PRO, purchases.toSubscriptionStatus())
    }

    @Test
    fun `only PENDING purchases still map to Free`() {
        val purchases = listOf(purchaseWithState(Purchase.PurchaseState.PENDING))

        assertEquals(SubscriptionStatus.FREE, purchases.toSubscriptionStatus())
    }

    @Test
    fun `a PENDING purchase alongside a PURCHASED one maps to Pro`() {
        val purchases = listOf(
            purchaseWithState(Purchase.PurchaseState.PENDING),
            purchaseWithState(Purchase.PurchaseState.PURCHASED)
        )

        assertEquals(SubscriptionStatus.PRO, purchases.toSubscriptionStatus())
    }
}
