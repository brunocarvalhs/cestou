package br.com.brunocarvalhs.howmuch.deeplink

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionLinkTest {

    private fun uri(scheme: String?, host: String?): Uri = mockk<Uri>(relaxed = true) {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.host } returns host
    }

    @Test
    fun `cestou subscription matches`() {
        assertTrue(SubscriptionLink.matches(uri("cestou", "subscription")))
    }

    @Test
    fun `home and other cestou links do not match`() {
        assertFalse(SubscriptionLink.matches(uri("cestou", "home")))
        assertFalse(SubscriptionLink.matches(uri("cestou", "shopping")))
    }

    @Test
    fun `other schemes do not match`() {
        assertFalse(SubscriptionLink.matches(uri("https", "subscription")))
    }
}
