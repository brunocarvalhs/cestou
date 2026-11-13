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
        every { editor.remove(any()) } returns editor
    }

    @Test
    fun `load returns FREE when no status is cached`() {
        every { prefs.getString("cached_subscription_owner", null) } returns null

        assertEquals(SubscriptionStatus.FREE, BillingStatusCache(context).load("uid-a"))
    }

    @Test
    fun `load returns PRO for the account that owns the cached PRO`() {
        every { prefs.getString("cached_subscription_owner", null) } returns "uid-a"
        every { prefs.getString("cached_subscription_status_v2", null) } returns "PRO"

        assertEquals(SubscriptionStatus.PRO, BillingStatusCache(context).load("uid-a"))
    }

    @Test
    fun `a cached PRO does not leak to another account`() {
        every { prefs.getString("cached_subscription_owner", null) } returns "uid-a"
        every { prefs.getString("cached_subscription_status_v2", null) } returns "PRO"

        assertEquals(SubscriptionStatus.FREE, BillingStatusCache(context).load("uid-b"))
    }

    @Test
    fun `load returns FREE when cached string is invalid enum`() {
        every { prefs.getString("cached_subscription_owner", null) } returns "uid-a"
        every { prefs.getString("cached_subscription_status_v2", null) } returns "INVALID_STATUS"

        assertEquals(SubscriptionStatus.FREE, BillingStatusCache(context).load("uid-a"))
    }

    @Test
    fun `save stores the owning account and the status`() {
        BillingStatusCache(context).save("uid-a", SubscriptionStatus.PRO)

        verify { editor.putString("cached_subscription_owner", "uid-a") }
        verify { editor.putString("cached_subscription_status_v2", "PRO") }
        verify { editor.remove("cached_subscription_status") }
        verify { editor.apply() }
    }
}
