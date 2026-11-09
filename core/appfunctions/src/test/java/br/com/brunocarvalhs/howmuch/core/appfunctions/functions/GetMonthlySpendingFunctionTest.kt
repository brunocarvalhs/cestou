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

class GetMonthlySpendingFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = GetMonthlySpendingFunction(
        shoppingRepository,
        productsUseCase,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `getMonthlySpending should return total spending`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        val products = listOf(Product("1", "Milk", 1.0, 50.0))
        coEvery { shoppingRepository.getAll() } returns listOf(shopping)
        coEvery { productsUseCase("1") } returns flowOf(products)

        val result = function.getMonthlySpending()
        assert(result.contains("spending"))
    }

    @Test
    fun `getMonthlySpending should handle empty lists`() = runTest {
        coEvery { shoppingRepository.getAll() } returns emptyList()
        coEvery { productsUseCase(any()) } returns flowOf(emptyList())

        val result = function.getMonthlySpending()
        assert(result.contains("spending"))
    }

    @Test
    fun `getMonthlySpending should handle error gracefully`() = runTest {
        coEvery { shoppingRepository.getAll() } throws Exception("Error")
        val result = function.getMonthlySpending()
        assert(result.contains("Failed"))
    }
}
