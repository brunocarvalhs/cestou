package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import io.ktor.client.network.sockets.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException

class AiErrorMessagesTest {

    @Test
    fun `network failures get the connectivity message`() {
        val ioMessage = aiErrorMessageFor(IOException("boom"))
        val timeoutMessage = aiErrorMessageFor(SocketTimeoutException("boom", cause = null))
        val hostMessage = aiErrorMessageFor(UnknownHostException("boom"))

        assertEquals(ioMessage, timeoutMessage)
        assertEquals(ioMessage, hostMessage)
        assertEquals(true, ioMessage.contains("conexão"))
    }

    @Test
    fun `non-network failures get a different, rephrase-oriented message`() {
        val decodeMessage = aiErrorMessageFor(SerializationException("bad json"))
        val networkMessage = aiErrorMessageFor(IOException("boom"))

        assertNotEquals(networkMessage, decodeMessage)
        assertEquals(true, decodeMessage.contains("reformular"))
    }

    @Test
    fun `error messages never leak the provider name`() {
        val message = aiErrorMessageFor(IllegalStateException("OpenRouter respondeu 402"))

        assertEquals(false, message.contains("OpenRouter", ignoreCase = true))
        assertEquals(false, message.contains("Gemini", ignoreCase = true))
    }
}
