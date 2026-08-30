package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingDuplicateUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingDuplicateUseCaseTest {

    private val context = mockk<Context> { every { getString(any()) } returns "copy" }
    private val repository = mockk<ShoppingRepository>(relaxed = true)
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val useCase = ShoppingDuplicateUseCase(context, repository, productsUseCase)

    private val original = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.FINISH,
        users = emptyList(),
        roles = emptyMap(),
        isFavorite = true
    )

    @Test
    fun `invoke creates a copy with a new id, NEW status and reset favorite flag`() = runTest {
        coEvery { productsUseCase(any()) } returns flowOf(emptyList())

        val result = useCase(original)

        assertTrue(result.isSuccess)
        val duplicated = result.getOrNull()!!
        assertNotEquals(original.id, duplicated.id)
        assertEquals("Weekly Groceries copy", duplicated.title)
        assertEquals(Shopping.Status.NEW, duplicated.status)
        assertEquals(false, duplicated.isFavorite)
        coVerify { repository.create(duplicated) }
    }

    @Test
    fun `invoke duplicates every product into the new list with a fresh id`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0, isPurchased = true)
        coEvery { productsUseCase(original.id) } returns flowOf(listOf(product))

        val result = useCase(original)

        assertTrue(result.isSuccess)
        val newShoppingId = result.getOrNull()!!.id
        coVerify {
            productsUseCase.update(
                match { it.id != "p1" && it.name == "Milk" && !it.isPurchased },
                newShoppingId
            )
        }
    }
}
