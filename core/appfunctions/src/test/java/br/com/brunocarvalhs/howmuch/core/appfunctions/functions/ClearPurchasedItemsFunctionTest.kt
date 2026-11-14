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

class ClearPurchasedItemsFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = ClearPurchasedItemsFunction(
        shoppingRepository,
        productsUseCase,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `clearPurchasedItems should remove purchased items`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        val products = listOf(
            Product("1", "Milk", 1.0, 5.0, isPurchased = true),
            Product("2", "Bread", 1.0, 3.0, isPurchased = false)
        )
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { productsUseCase("1") } returns flowOf(products)
        coEvery { productsUseCase.delete(any(), "1") } returns Result.success(Unit)

        val result = function.clearPurchasedItems("1")
        assert(result.contains("Removed"))
    }

    @Test
    fun `clearPurchasedItems should return error when list not found`() = runTest {
        coEvery { shoppingRepository.getById("1") } returns null
        val result = function.clearPurchasedItems("1")
        assert(result.contains("not found"))
    }

    @Test
    fun `clearPurchasedItems should handle error gracefully`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { productsUseCase("1") } throws Exception("Error")
        val result = function.clearPurchasedItems("1")
        assert(result.contains("Failed"))
    }
}
