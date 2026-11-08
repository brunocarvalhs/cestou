package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DuplicateShoppingListFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = DuplicateShoppingListFunction(
        shoppingRepository,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `duplicateShoppingList should duplicate with custom title`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { shoppingRepository.create(any()) } returns Unit

        val result = function.duplicateShoppingList("1", "Weekly2")
        assert(result.contains("duplicated"))
    }

    @Test
    fun `duplicateShoppingList should return error when list not found`() = runTest {
        coEvery { shoppingRepository.getById("1") } returns null
        val result = function.duplicateShoppingList("1")
        assert(result.contains("not found"))
    }

    @Test
    fun `duplicateShoppingList should handle error gracefully`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { shoppingRepository.create(any()) } throws Exception("Error")
        val result = function.duplicateShoppingList("1")
        assert(result.contains("Failed"))
    }
}
