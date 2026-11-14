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

class JoinShoppingListFunctionTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = JoinShoppingListFunction(
        shoppingRepository = shoppingRepository,
        analyticsTracker = analyticsTracker,
        crashlytics = crashlytics
    )

    @Test
    fun `joinShoppingList should join successfully`() = runTest {
        val shopping = Shopping(
            id = "shopping-1",
            title = "Weekly",
            description = "",
            price = 0.0,
            status = Shopping.Status.NEW,
            users = emptyList(),
            roles = emptyMap()
        )

        coEvery { shoppingRepository.resolveShortCode("ABC123") } returns "shopping-1"
        coEvery { shoppingRepository.getById("shopping-1") } returns shopping
        coEvery { shoppingRepository.join("shopping-1", any()) } returns Unit

        val result = function.joinShoppingList("ABC123")

        assert(result.contains("Joined"))
        coVerify { shoppingRepository.join("shopping-1", any()) }
    }

    @Test
    fun `joinShoppingList should return error for blank code`() = runTest {
        val result = function.joinShoppingList("   ")

        assert(result.contains("empty"))
    }

    @Test
    fun `joinShoppingList should return error for invalid code`() = runTest {
        coEvery { shoppingRepository.resolveShortCode("INVALID") } returns null

        val result = function.joinShoppingList("INVALID")

        assert(result.contains("Invalid"))
    }
}
