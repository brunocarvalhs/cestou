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

class UpdateProductPriceFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = UpdateProductPriceFunction(
        shoppingRepository,
        productsUseCase,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `updateProductPrice should update price`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        val product = Product("p1", "Milk", 1.0, 5.0)
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { productsUseCase("1") } returns flowOf(listOf(product))
        coEvery { productsUseCase.update(any(), "1") } returns Result.success(Unit)

        val result = function.updateProductPrice("1", "p1", 6.5f)
        assert(result.contains("updated"))
    }

    @Test
    fun `updateProductPrice should return error for negative price`() = runTest {
        val result = function.updateProductPrice("1", "p1", -5f)
        assert(result.contains("negative"))
    }

    @Test
    fun `updateProductPrice should return error when list not found`() = runTest {
        coEvery { shoppingRepository.getById("1") } returns null
        val result = function.updateProductPrice("1", "p1", 5f)
        assert(result.contains("not found"))
    }
}
