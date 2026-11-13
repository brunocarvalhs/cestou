package br.com.brunocarvalhs.howmuch.deeplink

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WearPairingLinkTest {

    private fun uri(
        scheme: String? = "cestou",
        host: String? = "wear",
        path: String? = "/pair",
        params: Map<String, String?> = mapOf("sid" to "session-1", "k" to "key-b64", "n" to "node-1")
    ): Uri = mockk<Uri>(relaxed = true) {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.host } returns host
        every { this@mockk.path } returns path
        params.forEach { (name, value) -> every { getQueryParameter(name) } returns value }
    }

    @Test
    fun `a complete pairing link is read into the route`() {
        val pairing = WearPairingLink.parse(uri())

        assertEquals("session-1", pairing?.sessionId)
        assertEquals("key-b64", pairing?.wearPublicKey)
        assertEquals("node-1", pairing?.wearNodeId)
    }

    @Test
    fun `another scheme is not a pairing link`() {
        assertNull(WearPairingLink.parse(uri(scheme = "https")))
    }

    @Test
    fun `another path under the same host is not a pairing link`() {
        assertNull(WearPairingLink.parse(uri(path = "/join")))
    }

    @Test
    fun `a link without the session id is refused`() {
        val link = mockk<Uri>(relaxed = true)
        every { link.scheme } returns "cestou"
        every { link.host } returns "wear"
        every { link.path } returns "/pair"
        every { link.getQueryParameter("sid") } returns null
        every { link.getQueryParameter("k") } returns "key"
        every { link.getQueryParameter("n") } returns "node"

        assertNull(WearPairingLink.parse(link))
    }

    @Test
    fun `a blank key is refused`() {
        assertNull(WearPairingLink.parse(uri(params = mapOf("sid" to "s", "k" to " ", "n" to "n"))))
    }
}
