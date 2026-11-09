package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecipeToShoppingListFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = RecipeToShoppingListFunction(
        shoppingRepository,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `recipeToShoppingList should add recipe ingredients`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping

        val result = function.recipeToShoppingList("1", "chocolate cake")
        assert(result.contains("chocolate cake"))
    }

    @Test
    fun `recipeToShoppingList should return error for empty recipe`() = runTest {
        val result = function.recipeToShoppingList("1", "")
        assert(result.contains("empty"))
    }

    @Test
    fun `recipeToShoppingList should return error when list not found`() = runTest {
        coEvery { shoppingRepository.getById("1") } returns null
        val result = function.recipeToShoppingList("1", "cake")
        assert(result.contains("not found"))
    }
}
