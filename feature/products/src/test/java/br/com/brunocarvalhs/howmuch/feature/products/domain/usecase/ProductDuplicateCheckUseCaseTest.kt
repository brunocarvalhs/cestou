package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
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

    @Test
    fun `invoke returns the existing active product on an exact match`() = runTest {
        val existing = Product(id = "p1", name = "Leite", quantity = 1.0)
        coEvery { repository.getAllProducts("list1") } returns flowOf(listOf(existing))

        val result = useCase("Leite", "list1")

        assertEquals(existing, result)
    }

    @Test
    fun `invoke matches case-insensitively and trims both sides`() = runTest {
        val existing = Product(id = "p1", name = "Leite ", quantity = 1.0)
        coEvery { repository.getAllProducts("list1") } returns flowOf(listOf(existing))

        val result = useCase(" leite", "list1")

        assertEquals(existing, result)
    }

    @Test
    fun `invoke returns null when the only match is already purchased`() = runTest {
        val purchased = Product(id = "p1", name = "Leite", quantity = 1.0, isPurchased = true)
        coEvery { repository.getAllProducts("list1") } returns flowOf(listOf(purchased))

        val result = useCase("Leite", "list1")

        assertNull(result)
    }

    @Test
    fun `invoke returns null when there is no name match`() = runTest {
        val existing = Product(id = "p1", name = "Leite", quantity = 1.0)
        coEvery { repository.getAllProducts("list1") } returns flowOf(listOf(existing))

        val result = useCase("Arroz", "list1")

        assertNull(result)
    }

    @Test
    fun `invoke returns null for a blank name without querying the repository`() = runTest {
        val result = useCase("   ", "list1")

        assertNull(result)
    }
}
