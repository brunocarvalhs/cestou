package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddProductsMultipleFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = AddProductsMultipleFunction(
        shoppingRepository,
        productsUseCase,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `addProductsMultiple should add multiple products`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 0.0, Shopping.Status.NEW, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { productsUseCase.save(any(), "1") } returns Result.success(Unit)

        val json = "Milk,1,5.0,Food;Bread,1,3.0,Food"
        val result = function.addProductsMultiple("1", json)
        assert(result.contains("Added"))
    }

    @Test
    fun `addProductsMultiple should return error for empty list`() = runTest {
        val result = function.addProductsMultiple("1", "")
        assert(result.contains("empty"))
    }

    @Test
    fun `addProductsMultiple should return error when list not found`() = runTest {
        coEvery { shoppingRepository.getById("1") } returns null
        val result = function.addProductsMultiple("1", "Milk,1,5.0")
        assert(result.contains("not found"))
    }
}
