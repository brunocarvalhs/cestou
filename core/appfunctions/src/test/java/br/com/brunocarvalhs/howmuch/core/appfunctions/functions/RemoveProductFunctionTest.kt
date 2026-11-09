package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemoveProductFunctionTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = RemoveProductFunction(
        shoppingRepository = shoppingRepository,
        productsUseCase = productsUseCase,
        analyticsTracker = analyticsTracker,
        crashlytics = crashlytics
    )

    @Test
    fun `removeProduct should delete product successfully`() = runTest {
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
        coEvery { productsUseCase.delete("product-1", "shopping-1") } returns Result.success(Unit)

        val result = function.removeProduct("shopping-1", "product-1")

        assert(result.contains("removed successfully"))
        coVerify { productsUseCase.delete("product-1", "shopping-1") }
    }

    @Test
    fun `removeProduct should return error when shopping list not found`() = runTest {
        coEvery { shoppingRepository.getById("invalid") } returns null

        val result = function.removeProduct("invalid", "product-1")

        assert(result.contains("not found"))
    }

    @Test
    fun `removeProduct should return error on delete failure`() = runTest {
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
        coEvery { productsUseCase.delete(any(), any()) } returns Result.failure(Exception("Delete failed"))

        val result = function.removeProduct("shopping-1", "product-1")

        assert(result.contains("Error"))
    }
}
