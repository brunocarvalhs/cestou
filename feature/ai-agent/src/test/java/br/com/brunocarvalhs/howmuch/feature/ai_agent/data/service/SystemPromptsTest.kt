package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val NON_STRING_SHOPPING_ID = 123

class SystemPromptsTest {

    @Test
    fun `contextSuffix includes the shopping id so the model doesn't have to ask for it`() {
        val suffix = SystemPrompts.contextSuffix(mapOf("shopping_id" to "list-123"))

        assertTrue(suffix.contains("list-123"))
    }

    @Test
    fun `contextSuffix is empty when there is no shopping id in the metadata`() {
        assertEquals("", SystemPrompts.contextSuffix(emptyMap()))
    }

    @Test
    fun `contextSuffix is empty when the shopping id is blank`() {
        assertEquals("", SystemPrompts.contextSuffix(mapOf("shopping_id" to "   ")))
    }

    @Test
    fun `contextSuffix is empty when the shopping id is not a String`() {
        assertEquals("", SystemPrompts.contextSuffix(mapOf("shopping_id" to NON_STRING_SHOPPING_ID)))
    }
}
