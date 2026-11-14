package br.com.brunocarvalhs.howmuch.core.appfunctions.functions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ListListMembersFunctionTest {
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    private val function = ListListMembersFunction(
        shoppingRepository,
        analyticsTracker,
        crashlytics
    )

    @Test
    fun `listListMembers should list members with roles`() = runTest {
        val roles = mapOf("user1" to "owner", "user2" to "editor")
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, listOf("user1", "user2"), roles)
        coEvery { shoppingRepository.getById("1") } returns shopping

        val result = function.listListMembers("1")
        assert(result.contains("2 members"))
    }

    @Test
    fun `listListMembers should return empty when no members`() = runTest {
        val shopping = Shopping("1", "Weekly", "", 100.0, Shopping.Status.NEW, emptyList(), emptyMap())
        coEvery { shoppingRepository.getById("1") } returns shopping

        val result = function.listListMembers("1")
        assert(result.contains("No members"))
    }

    @Test
    fun `listListMembers should return error when list not found`() = runTest {
        coEvery { shoppingRepository.getById("1") } returns null
        val result = function.listListMembers("1")
        assert(result.contains("not found"))
    }
}
