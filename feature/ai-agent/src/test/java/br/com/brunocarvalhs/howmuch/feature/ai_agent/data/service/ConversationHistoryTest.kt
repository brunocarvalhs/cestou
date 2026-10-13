package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import org.junit.Assert.assertEquals
import org.junit.Test

private fun turn(text: String, role: String = "user") =
    Content(role = role, parts = listOf(TextPart(text)))

private const val HISTORY_LIMIT = 20
private const val UNDER_LIMIT_HISTORY_SIZE = 5
private const val OVER_LIMIT_HISTORY_SIZE = 25
private const val DEFAULT_LIMIT_OVERFLOW = 3

class ConversationHistoryTest {

    @Test
    fun `trimToRecentHistory leaves a history smaller than the limit untouched`() {
        val history = (1..UNDER_LIMIT_HISTORY_SIZE).map { turn("msg $it") }.toMutableList()

        history.trimToRecentHistory(maxEntries = HISTORY_LIMIT)

        assertEquals(UNDER_LIMIT_HISTORY_SIZE, history.size)
    }

    @Test
    fun `trimToRecentHistory leaves a history exactly at the limit untouched`() {
        val history = (1..HISTORY_LIMIT).map { turn("msg $it") }.toMutableList()

        history.trimToRecentHistory(maxEntries = HISTORY_LIMIT)

        assertEquals(HISTORY_LIMIT, history.size)
    }

    @Test
    fun `trimToRecentHistory drops the oldest entries once over the limit`() {
        val history = (1..OVER_LIMIT_HISTORY_SIZE).map { turn("msg $it") }.toMutableList()

        history.trimToRecentHistory(maxEntries = HISTORY_LIMIT)

        assertEquals(HISTORY_LIMIT, history.size)
        // The oldest 5 are gone; what's left is a contiguous, still-ordered recent window.
        assertEquals("msg 6", (history.first().parts.first() as TextPart).text)
        assertEquals("msg 25", (history.last().parts.first() as TextPart).text)
    }

    @Test
    fun `trimToRecentHistory uses MAX_HISTORY_ENTRIES by default`() {
        val history = (1..(MAX_HISTORY_ENTRIES + DEFAULT_LIMIT_OVERFLOW)).map { turn("msg $it") }.toMutableList()

        history.trimToRecentHistory()

        assertEquals(MAX_HISTORY_ENTRIES, history.size)
    }
}
