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

class DuplicateProductFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = DuplicateProductFunction(
        shoppingRepository,
        productsUseCase,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `duplicateProduct should duplicate with quantity`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        val product = Product("p1", "Milk", 1.0, 5.0)
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { productsUseCase("1") } returns flowOf(listOf(product))
        coEvery { productsUseCase.save(any(), "1") } returns Result.success(Unit)

        val result = function.duplicateProduct("1", "p1", 2.0)
        assert(result.contains("duplicated"))
    }

    @Test
    fun `duplicateProduct should return error for zero quantity`() = runTest {
        val result = function.duplicateProduct("1", "p1", 0.0)
        assert(result.contains("greater than"))
    }

    @Test
    fun `duplicateProduct should return error when product not found`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { productsUseCase("1") } returns flowOf(emptyList())

        val result = function.duplicateProduct("1", "invalid", 1.0)
        assert(result.contains("not found"))
    }
}
