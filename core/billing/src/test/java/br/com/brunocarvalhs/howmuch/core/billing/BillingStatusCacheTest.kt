package br.com.brunocarvalhs.howmuch.core.billing

import android.content.Context
import android.content.SharedPreferences
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BillingStatusCacheTest {

    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val context = mockk<Context>()

    @Before
    fun setUp() {
        every { context.getSharedPreferences("billing_preferences", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
    }

    @Test
    fun `load returns FREE when no status is cached`() {
        every { prefs.getString("cached_subscription_status", null) } returns null

        val cache = BillingStatusCache(context)

        assertEquals(SubscriptionStatus.FREE, cache.load())
    }

    @Test
    fun `load returns PRO when PRO is cached`() {
        every { prefs.getString("cached_subscription_status", null) } returns "PRO"

        val cache = BillingStatusCache(context)

        assertEquals(SubscriptionStatus.PRO, cache.load())
    }

    @Test
    fun `load returns FREE when cached string is invalid enum`() {
        every { prefs.getString("cached_subscription_status", null) } returns "INVALID_STATUS"

        val cache = BillingStatusCache(context)

        assertEquals(SubscriptionStatus.FREE, cache.load())
    }

    @Test
    fun `save stores status in SharedPreferences`() {
        val cache = BillingStatusCache(context)

        cache.save(SubscriptionStatus.PRO)

        verify { editor.putString("cached_subscription_status", "PRO") }
        verify { editor.apply() }
    }
}
