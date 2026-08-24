package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ShoppingClearPurchasedUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingClearPurchasedUseCaseTest {

    private val repository = mockk<ProductRepository>(relaxed = true)
    private val useCase = ShoppingClearPurchasedUseCase(repository)

    @Test
    fun `invoke deletes only purchased products`() = runTest {
        coEvery { repository.getAllProducts("list1") } returns flowOf(
            listOf(
                Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0, isPurchased = true),
                Product(id = "p2", name = "Eggs", quantity = 1.0, price = 2.0, isPurchased = false)
            )
        )

        val result = useCase("list1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.deleteProduct("p1", "list1") }
        coVerify(exactly = 0) { repository.deleteProduct("p2", "list1") }
    }

    @Test
    fun `execute throws when shopping_id is missing`() {
        assertThrows(Exception::class.java) {
            runTest {
                useCase.execute(emptyMap(), mockk(relaxed = true), emptyMap())
            }
        }
    }
}
