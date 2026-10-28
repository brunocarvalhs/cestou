package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.User
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.text.ShoppingTexts
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingDuplicateUseCaseTest {

    private val texts = mockk<ShoppingTexts> { every { copySuffix() } returns "copy" }
    private val repository = mockk<ShoppingRepository>(relaxed = true)
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val authService = mockk<AuthService> {
        coEvery { getOrCreateUserId() } returns AuthenticatedUser(id = "me")
    }
    private val useCase = ShoppingDuplicateUseCase(texts, repository, productsUseCase, authService)

    private val original = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 42.0,
        status = Shopping.Status.FINISH,
        users = listOf("owner", "member"),
        roles = mapOf("owner" to "OWNER", "member" to "EDITOR"),
        isFavorite = true,
        shortCode = "ABC123",
        budget = BUDGET
    )

    @Test
    fun `invoke creates a private copy owned by the current user`() = runTest {
        coEvery { productsUseCase(any()) } returns flowOf(emptyList())

        val result = useCase(original)

        assertTrue(result.isSuccess)
        val duplicated = result.getOrNull()!!
        assertNotEquals(original.id, duplicated.id)
        assertEquals("Weekly Groceries copy", duplicated.title)
        assertEquals(Shopping.Status.NEW, duplicated.status)
        assertEquals(false, duplicated.isFavorite)
        assertEquals(0.0, duplicated.price, 0.0)
        assertEquals(listOf("me"), duplicated.users)
        assertEquals(mapOf("me" to User.Role.OWNER.name), duplicated.roles)
        assertNull(duplicated.shortCode)
        assertEquals(BUDGET, duplicated.budget)
        coVerify { repository.create(duplicated) }
    }

    @Test
    fun `invoke duplicates every product into the new list by saving, not updating`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0, isPurchased = true)
        coEvery { productsUseCase(original.id) } returns flowOf(listOf(product))

        val result = useCase(original)

        assertTrue(result.isSuccess)
        val newShoppingId = result.getOrNull()!!.id
        coVerify {
            productsUseCase.save(
                match { it.id != "p1" && it.name == "Milk" && !it.isPurchased },
                newShoppingId
            )
        }
        coVerify(exactly = 0) { productsUseCase.update(any(), any()) }
    }

    @Test
    fun `invoke fails without leaving a partial copy when a product cannot be saved`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        coEvery { productsUseCase(original.id) } returns flowOf(listOf(product))
        coEvery { productsUseCase.save(any(), any()) } returns Result.failure(IllegalStateException("locked"))

        val result = useCase(original)

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke reads products once instead of subscribing to the listener forever`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        // Mirrors getAllProducts(): a Firestore snapshot-listener Flow that emits once and never
        // completes on its own. Regression test for the old `.collect` that hung forever on this.
        val neverCompleting: Flow<List<Product>> = callbackFlow {
            trySend(listOf(product))
            awaitClose { }
        }
        coEvery { productsUseCase(original.id) } returns neverCompleting

        val result = useCase(original)

        assertTrue(result.isSuccess)
    }
}

private const val BUDGET = 100.0
