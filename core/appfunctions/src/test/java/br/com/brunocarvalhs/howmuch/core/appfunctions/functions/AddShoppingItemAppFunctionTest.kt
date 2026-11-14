package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddShoppingItemAppFunctionTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = AddShoppingItemAppFunction(
        shoppingRepository = shoppingRepository,
        productsUseCase = productsUseCase,
        analyticsTracker = analyticsTracker,
        crashlytics = crashlytics
    )

    @Test
    fun `addShoppingItem should save product successfully`() = runTest {
        // Arrange
        val shoppingId = "shopping-1"
        val shopping = Shopping(
            id = shoppingId,
            title = "Weekly Shopping",
            description = "",
            price = 0.0,
            status = Shopping.Status.NEW,
            users = emptyList(),
            roles = emptyMap()
        )
        val productName = "Milk"
        val price = 5.0f
        val quantity = 2.0f

        coEvery { shoppingRepository.getById(shoppingId) } returns shopping
        coEvery { productsUseCase.save(any(), shoppingId) } returns Result.success(Unit)

        // Act
        val result = function.addShoppingItem(shoppingId, productName, price, quantity)

        // Assert
        assert(result.contains("added successfully"))
        coVerify { productsUseCase.save(any(), shoppingId) }
    }

    @Test
    fun `addShoppingItem should return error when shopping list not found`() = runTest {
        // Arrange
        val shoppingId = "invalid-id"
        coEvery { shoppingRepository.getById(shoppingId) } returns null

        // Act
        val result = function.addShoppingItem(shoppingId, "Milk", 5.0f, 2.0f)

        // Assert
        assert(result.contains("not found"))
    }

    @Test
    fun `addShoppingItem should use default price and quantity`() = runTest {
        // Arrange
        val shoppingId = "shopping-1"
        val shopping = Shopping(
            id = shoppingId,
            title = "Weekly Shopping",
            description = "",
            price = 0.0,
            status = Shopping.Status.NEW,
            users = emptyList(),
            roles = emptyMap()
        )

        coEvery { shoppingRepository.getById(shoppingId) } returns shopping
        coEvery { productsUseCase.save(any(), shoppingId) } returns Result.success(Unit)

        // Act
        val result = function.addShoppingItem(shoppingId, "Milk")

        // Assert
        assert(result.contains("added successfully"))
    }
}
