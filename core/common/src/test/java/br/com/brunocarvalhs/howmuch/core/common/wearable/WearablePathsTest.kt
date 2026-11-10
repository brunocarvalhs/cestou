package br.com.brunocarvalhs.howmuch.core.common.wearable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WearablePathsTest {

    @Test
    fun `auth pair round trips the session, user and token`() {
        val decoded = WearablePaths.decodeAuthPair(
            WearablePaths.encodeAuthPair("session-1", "user-1", "token-abc")
        )

        assertEquals(Triple("session-1", "user-1", "token-abc"), decoded)
    }

    @Test
    fun `auth pair without a session is malformed`() {
        // The pre-sdd-055 payload (uid + token) must not be accepted as a sign-in.
        assertNull(WearablePaths.decodeAuthPair("user-1\ntoken-abc".toByteArray()))
    }

    @Test
    fun `auth pair with an empty field is malformed`() {
        assertNull(WearablePaths.decodeAuthPair("session-1\n\ntoken-abc".toByteArray()))
    }

    @Test
    fun `phone key round trips the session and the key`() {
        val decoded = WearablePaths.decodePhoneKey(WearablePaths.encodePhoneKey("session-1", "key-b64"))

        assertEquals("session-1" to "key-b64", decoded)
    }

    @Test
    fun `phone key with extra fields is malformed`() {
        assertNull(WearablePaths.decodePhoneKey("session-1\nkey\nextra".toByteArray()))
    }

    @Test
    fun `wear confirm round trips the session`() {
        assertEquals("session-1", WearablePaths.decodeWearConfirm(WearablePaths.encodeWearConfirm("session-1")))
    }

    @Test
    fun `empty wear confirm is malformed`() {
        assertNull(WearablePaths.decodeWearConfirm(ByteArray(0)))
    }

    @Test
    fun `error round trips the session and the reason`() {
        val decoded = WearablePaths.decodeError(WearablePaths.encodeError("session-1", "TIMEOUT"))

        assertEquals("session-1" to "TIMEOUT", decoded)
    }

    @Test
    fun `single field payload is malformed for two-field messages`() {
        assertNull(WearablePaths.decodeError("single-part".toByteArray()))
    }
}
