package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSaveUseCaseTest {

    private val repository = mockk<ProductRepository>()
    private val authService = mockk<AuthService>()
    private val duplicateCheckUseCase = mockk<ProductDuplicateCheckUseCase>()
    private val useCase = ProductSaveUseCase(repository, authService, duplicateCheckUseCase)

    @Test
    fun `invoke by name and quantity builds and saves a new product with an ADDED entry for the current user`() =
        runTest {
            coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user1")
            coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

            val result = useCase(name = "Milk", quantity = 2.0, shoppingId = "list1")

            assertTrue(result.isSuccess)
            coVerify {
                repository.saveProduct(
                    match {
                        it.name == "Milk" &&
                            it.quantity == 2.0 &&
                            it.addedBy == "user1" &&
                            it.history.single().action == ProductActivity.Action.ADDED
                    },
                    "list1"
                )
            }
        }

    @Test
    fun `invoke by product appends an ADDED entry for the current user before saving`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user1")
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        val result = useCase(product, "list1")

        assertTrue(result.isSuccess)
        coVerify {
            repository.saveProduct(
                match { it.addedBy == "user1" && it.history.size == 1 },
                "list1"
            )
        }
    }

    @Test
    fun `execute reads name, quantity and price from arguments`() = runTest {
        coEvery { duplicateCheckUseCase("Milk", "list1") } returns null
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        val result = useCase.execute(
            mapOf("name" to "Milk", "quantity" to 2.0, "price" to 5.0, "shoppingId" to "list1"),
            mockk(relaxed = true) { every { userId } returns "session-user" },
            emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify {
            repository.saveProduct(match { it.name == "Milk" && it.quantity == 2.0 && it.price == 5.0 }, "list1")
        }
    }

    @Test
    fun `execute defaults quantity and price when absent`() = runTest {
        coEvery { duplicateCheckUseCase("Milk", "list1") } returns null
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        useCase.execute(
            mapOf("name" to "Milk", "shoppingId" to "list1"),
            mockk(relaxed = true) { every { userId } returns "session-user" },
            emptyMap()
        )

        coVerify {
            repository.saveProduct(match { it.quantity == 1.0 && it.price == 0.0 }, "list1")
        }
    }

    @Test
    fun `execute appends an ADDED entry using the session userId`() = runTest {
        coEvery { duplicateCheckUseCase("Milk", "list1") } returns null
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        val result = useCase.execute(
            mapOf("name" to "Milk", "shoppingId" to "list1"),
            mockk(relaxed = true) { every { userId } returns "session-user" },
            emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify {
            repository.saveProduct(match { it.addedBy == "session-user" }, "list1")
        }
    }

    @Test
    fun `execute falls back to AuthService when the session has no userId`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "fallback-user")
        coEvery { duplicateCheckUseCase("Milk", "list1") } returns null
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        val result = useCase.execute(
            mapOf("name" to "Milk", "shoppingId" to "list1"),
            mockk(relaxed = true) { every { userId } returns null },
            emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify {
            repository.saveProduct(match { it.addedBy == "fallback-user" }, "list1")
        }
    }

    @Test
    fun `execute still saves and surfaces a warning when an active duplicate exists`() = runTest {
        val duplicate = Product(
            id = "p0",
            name = "Leite",
            quantity = 1.0,
            price = 5.0,
            history = listOf(ProductActivity(userId = "user-a", action = ProductActivity.Action.ADDED))
        )
        coEvery { duplicateCheckUseCase("Milk", "list1") } returns duplicate
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        val result = useCase.execute(
            mapOf("name" to "Milk", "shoppingId" to "list1"),
            mockk(relaxed = true) { every { userId } returns "session-user" },
            emptyMap()
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.contains("Leite") == true)
        coVerify {
            repository.saveProduct(match { it.name == "Milk" && it.addedBy == "session-user" }, "list1")
        }
    }

    @Test
    fun `execute fails when name is missing`() = runTest {
        val result = useCase.execute(mapOf("shoppingId" to "list1"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isFailure)
    }

    @Test
    fun `execute fails when shoppingId is missing from both arguments and metadata`() = runTest {
        val result = useCase.execute(mapOf("name" to "Milk"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isFailure)
    }
}
