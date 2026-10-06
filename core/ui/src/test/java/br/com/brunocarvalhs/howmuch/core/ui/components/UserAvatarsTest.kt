package br.com.brunocarvalhs.howmuch.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec EPA-08: initials and stable color used when there is no photo. */
@Suppress("MagicNumber")
class UserAvatarsTest {

    @Test
    fun `initials are the first letters of the first and last words`() {
        assertEquals("AS", avatarInitials("Ana Souza"))
        assertEquals("MS", avatarInitials("Maria da Silva"))
        assertEquals("JS", avatarInitials("  joão   pedro  santos "))
    }

    @Test
    fun `a one-word name shows a single letter`() {
        assertEquals("R", avatarInitials("Raoni"))
    }

    @Test
    fun `no usable name gives no initials`() {
        assertNull(avatarInitials(null))
        assertNull(avatarInitials("   "))
    }

    @Test
    fun `initials use the first visible character, never half of a composed one`() {
        assertEquals("👩‍👩‍👧", avatarInitials("👩‍👩‍👧"))
        assertEquals("ÉÁ", avatarInitials("élia ávila"))
        assertEquals("李", avatarInitials("李雷"))
    }

    @Test
    fun `the color slot is stable for the same seed and always within the palette`() {
        val first = avatarColorSlot("user-1", paletteSize = 3)
        repeat(5) { assertEquals(first, avatarColorSlot("user-1", paletteSize = 3)) }

        listOf("", "a", "user-2", "Ana Souza", "x".repeat(100)).forEach { seed ->
            assertTrue(avatarColorSlot(seed, paletteSize = 3) in 0 until 3)
        }
    }

    @Test
    fun `different people get spread across the palette`() {
        val slots = (1..30).map { avatarColorSlot("user-$it", paletteSize = 3) }.toSet()

        assertEquals(setOf(0, 1, 2), slots)
    }
}
