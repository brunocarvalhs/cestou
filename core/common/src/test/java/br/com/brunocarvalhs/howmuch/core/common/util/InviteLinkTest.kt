package br.com.brunocarvalhs.howmuch.core.common.util

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class InviteLinkTest {

    @Test
    fun `build produces a link with the token as the last path segment`() {
        assertEquals("https://cestou.app/join/ABC123", InviteLink.build("ABC123"))
    }

    @Test
    fun `matches is true for our own invite links`() {
        assertTrue(InviteLink.matches(Uri.parse(InviteLink.build("ABC123"))))
    }

    @Test
    fun `matches is false for a link with an unrelated host`() {
        assertFalse(InviteLink.matches(Uri.parse("https://example.com/join/ABC123")))
    }

    @Test
    fun `matches is false for a link with an unrelated path`() {
        assertFalse(InviteLink.matches(Uri.parse("https://cestou.app/other/ABC123")))
    }

    @Test
    fun `tokenFrom reads the token from the path`() {
        assertEquals("ABC123", InviteLink.tokenFrom(Uri.parse(InviteLink.build("ABC123"))))
    }

    @Test
    fun `tokenFrom reads the token from a query parameter when present`() {
        assertEquals(
            "ABC123",
            InviteLink.tokenFrom(Uri.parse("https://cestou.app/join?token=ABC123"))
        )
    }

    @Test
    fun `extractToken returns a bare token unchanged`() {
        assertEquals("ABC123", InviteLink.extractToken("ABC123"))
    }

    @Test
    fun `extractToken returns null for blank input`() {
        assertNull(InviteLink.extractToken("   "))
    }

    @Test
    fun `extractToken reads the token out of a full invite link`() {
        assertEquals("ABC123", InviteLink.extractToken(InviteLink.build("ABC123")))
    }

    @Test
    fun `extractToken ignores a link from an unrelated host`() {
        assertNull(InviteLink.extractToken("https://example.com/join/ABC123"))
    }
}
