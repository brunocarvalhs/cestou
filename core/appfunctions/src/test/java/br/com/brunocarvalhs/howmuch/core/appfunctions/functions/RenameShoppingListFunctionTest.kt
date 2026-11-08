package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RenameShoppingListFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = RenameShoppingListFunction(
        shoppingRepository,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `renameShoppingList should rename successfully`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { shoppingRepository.update(any()) } returns Unit

        val result = function.renameShoppingList("1", "Natal")
        assert(result.contains("Natal"))
    }

    @Test
    fun `renameShoppingList should return error for empty title`() = runTest {
        val result = function.renameShoppingList("1", "")
        assert(result.contains("empty"))
    }

    @Test
    fun `renameShoppingList should return error when list not found`() = runTest {
        coEvery { shoppingRepository.getById("1") } returns null
        val result = function.renameShoppingList("1", "NewName")
        assert(result.contains("not found"))
    }
}
