package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReopenShoppingListFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = ReopenShoppingListFunction(
        shoppingRepository,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `reopenShoppingList should reopen closed list`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.FINISH, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { shoppingRepository.update(any()) } returns Unit

        val result = function.reopenShoppingList("1")
        assert(result.contains("reopened"))
    }

    @Test
    fun `reopenShoppingList should return error when already active`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping

        val result = function.reopenShoppingList("1")
        assert(result.contains("already active"))
    }

    @Test
    fun `reopenShoppingList should return error when list not found`() = runTest {
        coEvery { shoppingRepository.getById("1") } returns null
        val result = function.reopenShoppingList("1")
        assert(result.contains("not found"))
    }
}
