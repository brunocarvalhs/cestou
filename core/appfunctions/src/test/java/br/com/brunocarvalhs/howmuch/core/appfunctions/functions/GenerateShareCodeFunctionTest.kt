package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GenerateShareCodeFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = GenerateShareCodeFunction(
        shoppingRepository,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `generateShareCode should generate code`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { shoppingRepository.ensureShortCodeIndex(any()) } returns Unit

        val result = function.generateShareCode("1")
        assert(result.contains("Share code"))
    }

    @Test
    fun `generateShareCode should return error when list not found`() = runTest {
        coEvery { shoppingRepository.getById("1") } returns null
        val result = function.generateShareCode("1")
        assert(result.contains("not found"))
    }

    @Test
    fun `generateShareCode should handle error gracefully`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping
        coEvery { shoppingRepository.ensureShortCodeIndex(any()) } throws Exception("Index error")

        val result = function.generateShareCode("1")
        assert(result.contains("Failed"))
    }
}
