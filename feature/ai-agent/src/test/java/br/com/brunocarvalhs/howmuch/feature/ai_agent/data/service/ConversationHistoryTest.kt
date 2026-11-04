package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.MAX_HISTORY_ENTRIES
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.trimToRecentHistory
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
private const val TURN_BOUNDARY_WINDOW = 3
private const val FULL_TURN_HISTORY_SIZE = 4

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

    @Test
    fun `trimToRecentHistory never opens on a model turn, even if that grows the window`() {
        // The naive cutoff (index 2) would land on "model" (a functionCall reply mid-turn);
        // the real cutoff must advance to the next "user" turn at index 3.
        val history = mutableListOf(
            turn("user 1", role = "user"),
            turn("model 1", role = "model"),
            turn("tool 1", role = "tool"),
            turn("user 2", role = "user"),
            turn("model 2", role = "model")
        )

        history.trimToRecentHistory(maxEntries = TURN_BOUNDARY_WINDOW)

        assertEquals(2, history.size)
        assertEquals("user", history.first().role)
        assertEquals("user 2", (history.first().parts.first() as TextPart).text)
    }

    @Test
    fun `trimToRecentHistory keeps a history that already starts on a user turn untouched in shape`() {
        val history = mutableListOf(
            turn("user 1", role = "user"),
            turn("model 1", role = "model"),
            turn("user 2", role = "user"),
            turn("model 2", role = "model")
        )

        history.trimToRecentHistory(maxEntries = FULL_TURN_HISTORY_SIZE)

        assertEquals(FULL_TURN_HISTORY_SIZE, history.size)
        assertEquals("user", history.first().role)
    }
}
