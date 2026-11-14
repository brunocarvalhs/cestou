package br.com.brunocarvalhs.howmuch.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitionOffsetsTest {

    private val width = 1080

    @Test
    fun `going forward enters from the right and the old screen leaves to the left`() {
        assertEquals(width, TransitionOffsets.enterFromRight(width))
        assertEquals(-width, TransitionOffsets.exitToLeft(width))
    }

    @Test
    fun `going back enters from the left and the popped screen leaves to the right`() {
        assertEquals(-width, TransitionOffsets.enterFromLeft(width))
        assertEquals(width, TransitionOffsets.exitToRight(width))
    }

    @Test
    fun `enter and exit offsets of one direction are opposite`() {
        assertTrue(TransitionOffsets.enterFromRight(width) > 0)
        assertTrue(TransitionOffsets.exitToLeft(width) < 0)
    }
}
