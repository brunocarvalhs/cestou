package br.com.brunocarvalhs.howmuch.feature.auth.domain.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingHandshakeTest {

    private val wear = PairingHandshake.newKeyPair()
    private val phone = PairingHandshake.newKeyPair()

    @Test
    fun `both devices derive the same code from the same two keys`() {
        val onWatch = PairingHandshake.deriveCode(
            sharedSecret = PairingHandshake.sharedSecret(wear.private, phone.public.encoded),
            wearPublicKey = wear.public.encoded,
            phonePublicKey = phone.public.encoded
        )
        val onPhone = PairingHandshake.deriveCode(
            sharedSecret = PairingHandshake.sharedSecret(phone.private, wear.public.encoded),
            wearPublicKey = wear.public.encoded,
            phonePublicKey = phone.public.encoded
        )

        assertEquals(onWatch, onPhone)
    }

    @Test
    fun `a different phone key produces a different code`() {
        val intruder = PairingHandshake.newKeyPair()
        val honest = codeFor(wear, phone)
        val tampered = PairingHandshake.deriveCode(
            sharedSecret = PairingHandshake.sharedSecret(wear.private, intruder.public.encoded),
            wearPublicKey = wear.public.encoded,
            phonePublicKey = intruder.public.encoded
        )

        assertNotEquals(honest, tampered)
    }

    @Test
    fun `the code is 8 consonants shown as two groups of four`() {
        val code = codeFor(wear, phone)

        assertTrue(Regex("[${PairingHandshake.ALPHABET}]{4} [${PairingHandshake.ALPHABET}]{4}").matches(code))
    }

    @Test
    fun `the same inputs always give the same code`() {
        assertEquals(codeFor(wear, phone), codeFor(wear, phone))
    }

    @Test
    fun `bytes above the rejection threshold are skipped, not folded into the alphabet`() {
        val okm = ByteArray(16) { 255.toByte() } + ByteArray(8) { 0 }

        assertEquals("BBBBBBBB", PairingHandshake.lettersFrom(okm))
    }

    @Test
    fun `formats the code as two groups of four`() {
        assertEquals("BCDF GHJK", PairingHandshake.formatCode("BCDFGHJK"))
    }

    @Test
    fun `a valid public key round trips through base64url`() {
        val encoded = PairingHandshake.encodePublicKey(phone)

        assertTrue(PairingHandshake.decodePublicKey(encoded)!!.contentEquals(phone.public.encoded))
    }

    @Test
    fun `garbage is not a public key`() {
        assertNull(PairingHandshake.decodePublicKey("not-a-key"))
        assertNull(PairingHandshake.decodePublicKey(""))
    }

    @Test
    fun `session ids are url safe and unique`() {
        val first = PairingHandshake.newSessionId()
        val second = PairingHandshake.newSessionId()

        assertEquals(22, first.length)
        assertFalse(first.contains('\n'))
        assertTrue(first.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertNotEquals(first, second)
    }

    private fun codeFor(watch: java.security.KeyPair, phone: java.security.KeyPair): String =
        PairingHandshake.deriveCode(
            sharedSecret = PairingHandshake.sharedSecret(watch.private, phone.public.encoded),
            wearPublicKey = watch.public.encoded,
            phonePublicKey = phone.public.encoded
        )
}
