package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ListAllShoppingListsFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = ListAllShoppingListsFunction(
        shoppingRepository,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `listAllShoppingLists should list all lists`() = runTest {
        val lists = listOf(
            Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap()),
            Shopping("2", "Monthly", "", 200.0, Shopping.Status.NEW, emptyList(), emptyMap())
        )
        coEvery { shoppingRepository.getAll() } returns lists
        val result = function.listAllShoppingLists()
        assert(result.contains("2") && result.contains("lists"))
    }

    @Test
    fun `listAllShoppingLists should return empty message when no lists`() = runTest {
        coEvery { shoppingRepository.getAll() } returns emptyList()
        val result = function.listAllShoppingLists()
        assert(result.contains("No shopping lists"))
    }

    @Test
    fun `listAllShoppingLists should handle error gracefully`() = runTest {
        coEvery { shoppingRepository.getAll() } throws Exception("Database error")
        val result = function.listAllShoppingLists()
        assert(result.contains("Failed"))
    }
}
