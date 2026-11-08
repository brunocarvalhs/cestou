package br.com.brunocarvalhs.howmuch.feature.cart.presentation.helper

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import org.junit.Assert.assertEquals
import org.junit.Test

class CartTotalsTest {

    private fun product(id: String, price: Double?, quantity: Double = 1.0, purchased: Boolean = false) =
        Product(id = id, name = id, price = price, quantity = quantity, isPurchased = purchased)

    @Test
    fun `empty list has zero totals`() {
        assertEquals(CartTotals(), computeCartTotals(StableList()))
    }

    @Test
    fun `planned sums every item and inCart only purchased ones`() {
        val totals = computeCartTotals(
            StableList(
                listOf(
                    product("a", 10.0, quantity = 2.0, purchased = true),
                    product("b", 5.0),
                    product("c", 3.0, purchased = true)
                )
            )
        )

        assertEquals(28.0, totals.planned, 0.0)
        assertEquals(23.0, totals.inCart, 0.0)
        assertEquals(0, totals.unpricedCount)
    }

    @Test
    fun `items without price count as zero and are reported`() {
        val totals = computeCartTotals(
            StableList(listOf(product("a", null, purchased = true), product("b", 0.0), product("c", 4.0)))
        )

        assertEquals(4.0, totals.planned, 0.0)
        assertEquals(0.0, totals.inCart, 0.0)
        assertEquals(2, totals.unpricedCount)
    }

    @Test
    fun `unchecking an item removes it from the cart total`() {
        val checked = StableList(listOf(product("a", 10.0, purchased = true)))
        val unchecked = StableList(listOf(product("a", 10.0, purchased = false)))

        assertEquals(10.0, computeCartTotals(checked).inCart, 0.0)
        assertEquals(0.0, computeCartTotals(unchecked).inCart, 0.0)
    }
}
