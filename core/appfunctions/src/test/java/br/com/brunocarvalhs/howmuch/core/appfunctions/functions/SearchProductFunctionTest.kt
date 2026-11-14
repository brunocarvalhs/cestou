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

class SearchProductFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = SearchProductFunction(
        shoppingRepository,
        productsUseCase,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `searchProduct should find products by name`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 0.0, Shopping.Status.NEW, emptyList(), emptyMap())
        val products = listOf(
            Product("1", "Milk", 1.0, 5.0),
            Product("2", "Butter", 1.0, 3.0)
        )
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { productsUseCase("1") } returns flowOf(products)

        val result = function.searchProduct("1", "Milk")
        assert(result.contains("Milk"))
    }

    @Test
    fun `searchProduct should return error for empty query`() = runTest {
        val result = function.searchProduct("1", "")
        assert(result.contains("empty"))
    }

    @Test
    fun `searchProduct should return error when list not found`() = runTest {
        coEvery { shoppingRepository.getById("1") } returns null
        val result = function.searchProduct("1", "Milk")
        assert(result.contains("not found"))
    }
}
