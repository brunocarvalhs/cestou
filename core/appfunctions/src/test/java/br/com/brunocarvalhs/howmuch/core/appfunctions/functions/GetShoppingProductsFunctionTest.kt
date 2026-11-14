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

class GetShoppingProductsFunctionTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = GetShoppingProductsFunction(
        shoppingRepository = shoppingRepository,
        productsUseCase = productsUseCase,
        analyticsTracker = analyticsTracker,
        crashlytics = crashlytics
    )

    @Test
    fun `getShoppingProducts should return products list`() = runTest {
        val shopping = Shopping(
            id = "shopping-1",
            title = "Weekly",
            description = "",
            price = 0.0,
            status = Shopping.Status.NEW,
            users = emptyList(),
            roles = emptyMap()
        )
        val products = listOf(
            Product("1", "Milk", 1.0, 5.0, isPurchased = true),
            Product("2", "Bread", 1.0, 3.0, isPurchased = false)
        )

        coEvery { shoppingRepository.getById("shopping-1") } returns shopping
        coEvery { productsUseCase("shopping-1") } returns flowOf(products)

        val result = function.getShoppingProducts("shopping-1")

        assert(result.contains("Milk"))
        assert(result.contains("Bread"))
    }

    @Test
    fun `getShoppingProducts should return empty message when no products`() = runTest {
        val shopping = Shopping(
            id = "shopping-1",
            title = "Weekly",
            description = "",
            price = 0.0,
            status = Shopping.Status.NEW,
            users = emptyList(),
            roles = emptyMap()
        )

        coEvery { shoppingRepository.getById("shopping-1") } returns shopping
        coEvery { productsUseCase("shopping-1") } returns flowOf(emptyList())

        val result = function.getShoppingProducts("shopping-1")

        assert(result.contains("No products"))
    }

    @Test
    fun `getShoppingProducts should return error when shopping list not found`() = runTest {
        coEvery { shoppingRepository.getById("invalid") } returns null

        val result = function.getShoppingProducts("invalid")

        assert(result.contains("not found"))
    }
}
