package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

// Split out of ProductSaveUseCaseTest to keep TooManyFunctions under the detekt threshold —
// same fixture shape, scoped to the product_added(source=ai_chat) instrumentation on the
// AI-agent execute() path instead of the save behavior itself.
class ProductSaveUseCaseAnalyticsTest {

    private val repository = mockk<ProductRepository>()
    private val authService = mockk<AuthService>()
    private val duplicateCheckUseCase = mockk<ProductDuplicateCheckUseCase>()
    private val userRepository = mockk<UserRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val useCase = ProductSaveUseCase(
        repository, authService, duplicateCheckUseCase, userRepository, analyticsTracker
    )

    @Test
    fun `execute tracks product_added with source ai_chat after a successful save`() = runTest {
        coEvery { duplicateCheckUseCase("Milk", "list1") } returns null
        coEvery { repository.saveProduct(any(), "list1") } returns Result.success(Unit)

        useCase.execute(
            mapOf("name" to "Milk", "shoppingId" to "list1"),
            mockk(relaxed = true) { every { userId } returns "session-user" },
            emptyMap()
        )

        coVerify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.PRODUCT_ADDED,
                mapOf(
                    AnalyticsParams.SHOPPING_ID to "list1",
                    AnalyticsParams.SOURCE to "ai_chat"
                )
            )
        }
    }

    @Test
    fun `execute does not track product_added when the save fails`() = runTest {
        coEvery { duplicateCheckUseCase("Milk", "list1") } returns null
        coEvery { repository.saveProduct(any(), "list1") } returns Result.failure(Exception("offline"))

        useCase.execute(
            mapOf("name" to "Milk", "shoppingId" to "list1"),
            mockk(relaxed = true) { every { userId } returns "session-user" },
            emptyMap()
        )

        coVerify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.PRODUCT_ADDED, any()) }
    }
}
