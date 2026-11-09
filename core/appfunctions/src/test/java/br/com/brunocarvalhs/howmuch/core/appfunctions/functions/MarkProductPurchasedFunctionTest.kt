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

class MarkProductPurchasedFunctionTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = MarkProductPurchasedFunction(
        shoppingRepository = shoppingRepository,
        productsUseCase = productsUseCase,
        analyticsTracker = analyticsTracker,
        crashlytics = crashlytics
    )

    @Test
    fun `markProductPurchased should mark product as purchased`() = runTest {
        val shoppingId = "shopping-1"
        val productId = "product-1"
        val shopping = Shopping(
            id = shoppingId,
            title = "Weekly",
            description = "",
            price = 0.0,
            status = Shopping.Status.NEW,
            users = emptyList(),
            roles = emptyMap()
        )
        val product = Product(
            id = productId,
            name = "Milk",
            quantity = 1.0,
            price = 5.0,
            isPurchased = false
        )

        coEvery { shoppingRepository.getById(shoppingId) } returns shopping
        coEvery { productsUseCase(shoppingId) } returns flowOf(listOf(product))
        coEvery { productsUseCase.update(any(), shoppingId) } returns Result.success(Unit)

        val result = function.markProductPurchased(shoppingId, productId, true)

        assert(result.contains("purchased"))
        coVerify { productsUseCase.update(any(), shoppingId) }
    }

    @Test
    fun `markProductPurchased should return error when shopping list not found`() = runTest {
        coEvery { shoppingRepository.getById("invalid") } returns null

        val result = function.markProductPurchased("invalid", "prod-1", true)

        assert(result.contains("not found"))
    }

    @Test
    fun `markProductPurchased should return error when product not found`() = runTest {
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

        val result = function.markProductPurchased("shopping-1", "invalid-prod", true)

        assert(result.contains("not found"))
    }
}
