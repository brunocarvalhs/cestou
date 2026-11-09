package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CreateShoppingListFunctionTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = CreateShoppingListFunction(
        shoppingRepository = shoppingRepository,
        analyticsTracker = analyticsTracker,
        crashlytics = crashlytics
    )

    @Test
    fun `createShoppingList should create shopping list successfully`() = runTest {
        // Arrange
        val title = "Weekly Shopping"
        val budget = 100.0f

        coEvery { shoppingRepository.create(any()) } returns Unit

        // Act
        val result = function.createShoppingList(title, budget)

        // Assert
        assert(result.isNotEmpty())
        assert(!result.contains("Failed"))
        coVerify { shoppingRepository.create(any()) }
    }

    @Test
    fun `createShoppingList should return error for blank title`() = runTest {
        // Arrange
        val title = "   "

        // Act
        val result = function.createShoppingList(title)

        // Assert
        assert(result.contains("cannot be empty"))
    }

    @Test
    fun `createShoppingList should handle null budget`() = runTest {
        // Arrange
        val title = "Weekly Shopping"

        coEvery { shoppingRepository.create(any()) } returns Unit

        // Act
        val result = function.createShoppingList(title, null)

        // Assert
        assert(result.isNotEmpty())
        assert(!result.contains("Failed"))
    }
}
