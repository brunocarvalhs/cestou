package br.com.brunocarvalhs.howmuch.feature.cart.presentation.screen

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class CartGroupingTest {

    private val milk = Product(id = "1", name = "Milk", quantity = 1.0, category = "Laticínios")
    private val rice = Product(id = "2", name = "Rice", quantity = 1.0, category = "Mercearia", isPurchased = true)
    private val beans = Product(id = "3", name = "Beans", quantity = 1.0, category = "Mercearia")

    @Test
    fun `purchased items move to a trailing in-cart section`() {
        val grouped = groupCartProducts(listOf(milk, rice, beans), isCategorized = true, separateInCart = true)

        assertEquals(listOf("Laticínios", "Mercearia", IN_CART_SECTION_KEY), grouped.keys.toList())
        assertEquals(listOf(beans), grouped.getValue("Mercearia"))
        assertEquals(listOf(rice), grouped.getValue(IN_CART_SECTION_KEY))
    }

    @Test
    fun `no in-cart section when nothing is purchased`() {
        val grouped = groupCartProducts(listOf(milk, beans), isCategorized = true, separateInCart = true)

        assertEquals(listOf("Laticínios", "Mercearia"), grouped.keys.toList())
    }

    @Test
    fun `purchased items stay in their category when the flag is off`() {
        val grouped = groupCartProducts(listOf(milk, rice, beans), isCategorized = true, separateInCart = false)

        assertEquals(listOf("Laticínios", "Mercearia"), grouped.keys.toList())
        assertEquals(listOf(rice, beans), grouped.getValue("Mercearia"))
    }

    @Test
    fun `uncategorized list keeps one pending group plus the in-cart section`() {
        val grouped = groupCartProducts(listOf(milk, rice), isCategorized = false, separateInCart = true)

        assertEquals(listOf("", IN_CART_SECTION_KEY), grouped.keys.toList())
    }
}
