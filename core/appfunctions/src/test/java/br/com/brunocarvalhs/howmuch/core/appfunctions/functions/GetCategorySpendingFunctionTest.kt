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

class GetCategorySpendingFunctionTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = GetCategorySpendingFunction(
        shoppingRepository = shoppingRepository,
        productsUseCase = productsUseCase,
        analyticsTracker = analyticsTracker,
        crashlytics = crashlytics
    )

    @Test
    fun `getCategorySpending should return category breakdown`() = runTest {
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
            Product("1", "Milk", 1.0, 5.0, category = "Food"),
            Product("2", "Soap", 1.0, 3.0, category = "Hygiene")
        )

        coEvery { shoppingRepository.getById("shopping-1") } returns shopping
        coEvery { productsUseCase("shopping-1") } returns flowOf(products)

        val result = function.getCategorySpending("shopping-1")

        assert(result.contains("Food"))
        assert(result.contains("Hygiene"))
        assert(result.contains("Total"))
    }

    @Test
    fun `getCategorySpending should handle empty products`() = runTest {
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

        val result = function.getCategorySpending("shopping-1")

        assert(result.contains("No products"))
    }

    @Test
    fun `getCategorySpending should return error when shopping list not found`() = runTest {
        coEvery { shoppingRepository.getById("invalid") } returns null

        val result = function.getCategorySpending("invalid")

        assert(result.contains("not found"))
    }
}
