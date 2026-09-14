package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import org.junit.Assert.assertEquals
import org.junit.Test

private fun turn(text: String, role: String = "user") =
    Content(role = role, parts = listOf(TextPart(text)))

class ConversationHistoryTest {

    @Test
    fun `trimToRecentHistory leaves a history smaller than the limit untouched`() {
        val history = (1..5).map { turn("msg $it") }.toMutableList()

        history.trimToRecentHistory(maxEntries = 20)

        assertEquals(5, history.size)
    }

    @Test
    fun `trimToRecentHistory leaves a history exactly at the limit untouched`() {
        val history = (1..20).map { turn("msg $it") }.toMutableList()

        history.trimToRecentHistory(maxEntries = 20)

        assertEquals(20, history.size)
    }

    @Test
    fun `trimToRecentHistory drops the oldest entries once over the limit`() {
        val history = (1..25).map { turn("msg $it") }.toMutableList()

        history.trimToRecentHistory(maxEntries = 20)

        assertEquals(20, history.size)
        // The oldest 5 are gone; what's left is a contiguous, still-ordered recent window.
        assertEquals("msg 6", (history.first().parts.first() as TextPart).text)
        assertEquals("msg 25", (history.last().parts.first() as TextPart).text)
    }

    @Test
    fun `trimToRecentHistory uses MAX_HISTORY_ENTRIES by default`() {
        val history = (1..(MAX_HISTORY_ENTRIES + 3)).map { turn("msg $it") }.toMutableList()

        history.trimToRecentHistory()

        assertEquals(MAX_HISTORY_ENTRIES, history.size)
    }
}
