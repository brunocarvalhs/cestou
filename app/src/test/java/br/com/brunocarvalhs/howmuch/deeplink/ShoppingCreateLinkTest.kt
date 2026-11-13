package br.com.brunocarvalhs.howmuch.deeplink

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingCreateLinkTest {

    private fun uri(scheme: String?, host: String?, path: String?): Uri = mockk<Uri>(relaxed = true) {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.host } returns host
        every { this@mockk.path } returns path
    }

    @Test
    fun `cestou shopping new matches`() {
        assertTrue(ShoppingCreateLink.matches(uri("cestou", "shopping", "/new")))
    }

    @Test
    fun `other cestou links do not match`() {
        assertFalse(ShoppingCreateLink.matches(uri("cestou", "home", null)))
        assertFalse(ShoppingCreateLink.matches(uri("cestou", "shopping", "/other")))
        assertFalse(ShoppingCreateLink.matches(uri("cestou", "wear", "/pair")))
    }

    @Test
    fun `other schemes do not match`() {
        assertFalse(ShoppingCreateLink.matches(uri("https", "shopping", "/new")))
    }
}
