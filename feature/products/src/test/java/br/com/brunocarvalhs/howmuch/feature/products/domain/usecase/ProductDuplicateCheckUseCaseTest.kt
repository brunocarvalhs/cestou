package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.withActivity
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductDuplicateCheckUseCaseTest {

    private val repository = mockk<ProductRepository>()
    private val useCase = ProductDuplicateCheckUseCase(repository)

    private fun product(name: String, isPurchased: Boolean = false) =
        Product(id = name, name = name, quantity = 1.0, price = 1.0, isPurchased = isPurchased)
            .withActivity(ProductActivity.Action.ADDED, userId = "user-a")

    @Test
    fun `invoke matches an active product ignoring case and surrounding whitespace`() = runTest {
        val existing = product(name = "Leite ")
        coEvery { repository.getAllProducts("list1") } returns flowOf(listOf(existing))

        val result = useCase(name = "leite", shoppingId = "list1")

        assertEquals(existing, result)
    }

    @Test
    fun `invoke ignores a match that is already purchased`() = runTest {
        val purchased = product(name = "Leite", isPurchased = true)
        coEvery { repository.getAllProducts("list1") } returns flowOf(listOf(purchased))

        val result = useCase(name = "Leite", shoppingId = "list1")

        assertNull(result)
    }

    @Test
    fun `invoke returns null when there is no matching product`() = runTest {
        coEvery { repository.getAllProducts("list1") } returns flowOf(listOf(product(name = "Arroz")))

        val result = useCase(name = "Leite", shoppingId = "list1")

        assertNull(result)
    }
}
