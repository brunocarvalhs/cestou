package br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class SortProductsUseCaseTest {

    private val useCase = SortProductsUseCase()

    private val products = listOf(
        Product(id = "1", name = "Banana", category = "Frutas", isPurchased = true, price = 1.0, quantity = 1.0),
        Product(id = "2", name = "Abacaxi", category = "Frutas", isPurchased = false, price = 1.0, quantity = 1.0),
        Product(id = "3", name = "Leite", category = "Laticínios", isPurchased = false, price = 1.0, quantity = 1.0)
    )

    @Test
    fun `when sorting mode is NAME, should sort alphabetically`() {
        val result = useCase(products, "NAME")
        
        assertEquals("Abacaxi", result[0].name)
        assertEquals("Banana", result[1].name)
        assertEquals("Leite", result[2].name)
    }

    @Test
    fun `when sorting mode is DEFAULT, should sort by purchased status then category then name`() {
        val result = useCase(products, "DEFAULT")
        
        // Non-purchased items first
        assertEquals("Abacaxi", result[0].name) // category Frutas, name Abacaxi
        assertEquals("Leite", result[1].name)    // category Laticínios
        assertEquals("Banana", result[2].name)   // Purchased = true is last
    }
}
