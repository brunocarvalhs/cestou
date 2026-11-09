package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetShoppingListSummaryFunctionTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = GetShoppingListSummaryFunction(
        shoppingRepository = shoppingRepository,
        productsUseCase = productsUseCase,
        analyticsTracker = analyticsTracker,
        crashlytics = crashlytics
    )

    @Test
    fun `getShoppingListSummary should return correct summary`() = runTest {
        // Arrange
        val shoppingId = "shopping-1"
        val shopping = Shopping(
            id = shoppingId,
            title = "Weekly Shopping",
            description = "",
            price = 0.0,
            status = Shopping.Status.NEW,
            users = emptyList(),
            roles = emptyMap(),
            budget = 100.0
        )
        val products = listOf(
            Product(id = "1", name = "Milk", quantity = 2.0, price = 5.0, isPurchased = false),
            Product(id = "2", name = "Bread", quantity = 1.0, price = 3.0, isPurchased = true),
            Product(id = "3", name = "Butter", quantity = 1.0, price = 7.0, isPurchased = false)
        )

        coEvery { shoppingRepository.getById(shoppingId) } returns shopping
        coEvery { productsUseCase(shoppingId) } returns flowOf(products)

        // Act
        val result = function.getShoppingListSummary(shoppingId)

        // Assert
        assert(result.contains("Weekly Shopping"))
        assert(result.contains("Items: 3"))
        assert(result.contains("pending: 2"))
        assert(result.contains("purchased: 1"))
        assert(result.contains("Total"))
        assert(result.contains("Budget"))
    }

    @Test
    fun `getShoppingListSummary should return error when shopping list not found`() = runTest {
        // Arrange
        val shoppingId = "invalid-id"
        coEvery { shoppingRepository.getById(shoppingId) } returns null

        // Act
        val result = function.getShoppingListSummary(shoppingId)

        // Assert
        assert(result.contains("not found"))
    }

    @Test
    fun `getShoppingListSummary should handle empty product list`() = runTest {
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
        coEvery { productsUseCase(shoppingId) } returns flowOf(emptyList())

        // Act
        val result = function.getShoppingListSummary(shoppingId)

        // Assert
        assert(result.contains("Items: 0"))
    }
}
