package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.Agent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FallbackAgentTest {

    private val primary = mockk<Agent>()
    private val secondary = mockk<Agent>()
    private val context = mockk<AgentContext>()
    private val session = mockk<AiSession>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val agent = FallbackAgent(primary, secondary, session, analyticsTracker)

    @Test
    fun `sendMessage emits from primary when it succeeds`() = runTest {
        coEvery { primary.sendMessage("hi", context) } returns flow { emit("from primary") }

        val result = agent.sendMessage("hi", context).toList()

        assertEquals(listOf("from primary"), result)
    }

    @Test
    fun `sendMessage falls back to secondary when primary throws and no tool has run`() = runTest {
        every { session.toolExecutedThisTurn } returns false
        every { session.providerUsed } returns "gemini"
        coEvery { primary.sendMessage("hi", context) } returns flow { throw RuntimeException("boom") }
        coEvery { secondary.sendMessage("hi", context) } returns flow { emit("from secondary") }

        val result = agent.sendMessage("hi", context).toList()

        assertEquals(listOf("from secondary"), result)
        verify {
            analyticsTracker.trackEvent(AnalyticsEvents.AI_PROVIDER_FALLBACK, mapOf(AnalyticsParams.FROM to "gemini"))
        }
        verify { session.fallbackUsedThisTurn = true }
    }

    @Test
    fun `sendMessage does not retry the secondary when a tool already ran, to avoid duplicating it`() = runTest {
        // sdd-012: a tool with a real side effect (e.g. a product already written to Firestore)
        // must never be re-run on the secondary provider just because the follow-up network call
        // on the primary failed.
        every { session.toolExecutedThisTurn } returns true
        coEvery { primary.sendMessage("hi", context) } returns flow { throw RuntimeException("boom") }

        val error = runCatching { agent.sendMessage("hi", context).toList() }.exceptionOrNull()

        assertEquals("boom", error?.message)
        coVerify(exactly = 0) { secondary.sendMessage(any(), any()) }
        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.AI_PROVIDER_FALLBACK, any()) }
    }
}
