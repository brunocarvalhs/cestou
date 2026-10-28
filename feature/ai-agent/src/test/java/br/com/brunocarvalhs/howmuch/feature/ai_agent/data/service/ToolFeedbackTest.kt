package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import org.junit.Assert.assertEquals
import org.junit.Test

private const val SOME_ID = 123

class ToolFeedbackTest {

    @Test
    fun `a failure becomes an explicit error, never a false success`() {
        val feedback = Result.failure<Any?>(Exception("not found")).toToolFeedback()

        assertEquals("Erro: not found", feedback)
    }

    @Test
    fun `a Unit success becomes OK, not the literal kotlin-Unit`() {
        val feedback = Result.success(Unit).toToolFeedback()

        assertEquals("OK", feedback)
    }

    @Test
    fun `a null success becomes OK too`() {
        val feedback = Result.success<Any?>(null).toToolFeedback()

        assertEquals("OK", feedback)
    }

    @Test
    fun `a value success keeps its textual representation`() {
        val feedback = Result.success<Any?>(SOME_ID).toToolFeedback()

        assertEquals(SOME_ID.toString(), feedback)
    }
}
