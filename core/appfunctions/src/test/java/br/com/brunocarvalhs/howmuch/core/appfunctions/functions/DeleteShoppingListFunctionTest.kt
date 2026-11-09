package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteShoppingListFunctionTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = DeleteShoppingListFunction(
        shoppingRepository = shoppingRepository,
        analyticsTracker = analyticsTracker,
        crashlytics = crashlytics
    )

    @Test
    fun `deleteShoppingList should delete successfully`() = runTest {
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
        coEvery { shoppingRepository.delete(shopping) } returns Unit

        val result = function.deleteShoppingList("shopping-1")

        assert(result.contains("deleted successfully"))
        coVerify { shoppingRepository.delete(shopping) }
    }

    @Test
    fun `deleteShoppingList should return error when not found`() = runTest {
        coEvery { shoppingRepository.getById("invalid") } returns null

        val result = function.deleteShoppingList("invalid")

        assert(result.contains("not found"))
    }

    @Test
    fun `deleteShoppingList should handle delete error`() = runTest {
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
        coEvery { shoppingRepository.delete(any()) } throws Exception("Delete failed")

        val result = function.deleteShoppingList("shopping-1")

        assert(result.contains("Failed"))
    }
}
