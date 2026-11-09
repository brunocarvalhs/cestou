package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateShoppingBudgetFunctionTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = UpdateShoppingBudgetFunction(
        shoppingRepository = shoppingRepository,
        productsUseCase = productsUseCase,
        analyticsTracker = analyticsTracker,
        crashlytics = crashlytics
    )

    @Test
    fun `updateShoppingBudget should update budget successfully`() = runTest {
        val shopping = Shopping(
            id = "shopping-1",
            title = "Weekly",
            description = "",
            price = 0.0,
            status = Shopping.Status.NEW,
            users = emptyList(),
            roles = emptyMap(),
            budget = 100.0
        )
        val products = listOf(
            Product("1", "Milk", 1.0, 5.0),
            Product("2", "Bread", 1.0, 3.0)
        )

        coEvery { shoppingRepository.getById("shopping-1") } returns shopping
        coEvery { shoppingRepository.update(any()) } returns Unit
        coEvery { productsUseCase("shopping-1") } returns flowOf(products)

        val result = function.updateShoppingBudget("shopping-1", 150f)

        assert(result.contains("Budget updated"))
        coVerify { shoppingRepository.update(any()) }
    }

    @Test
    fun `updateShoppingBudget should return error for negative budget`() = runTest {
        val result = function.updateShoppingBudget("shopping-1", -10f)

        assert(result.contains("negative"))
    }

    @Test
    fun `updateShoppingBudget should return error when shopping list not found`() = runTest {
        coEvery { shoppingRepository.getById("invalid") } returns null

        val result = function.updateShoppingBudget("invalid", 100f)

        assert(result.contains("not found"))
    }
}
