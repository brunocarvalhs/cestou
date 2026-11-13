package br.com.brunocarvalhs.howmuch.core.common.wearable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Codec do relay de IA do relógio. O texto livre é o último campo e pode ter quebra de linha. */
class WearAiCodecTest {

    @Test
    fun `prompt keeps a line break inside the free text`() {
        val payload = WearAiCodec.encodePrompt("req-1", "adicionar leite\ne pão")

        assertEquals("req-1" to "adicionar leite\ne pão", WearAiCodec.decodePrompt(payload))
    }

    @Test
    fun `prompt with a blank text is rejected`() {
        val payload = WearAiCodec.encodePrompt("req-1", "   ")

        assertNull(WearAiCodec.decodePrompt(payload))
    }

    @Test
    fun `prompt with a blank request id is rejected`() {
        val payload = WearAiCodec.encodePrompt("", "adicionar leite")

        assertNull(WearAiCodec.decodePrompt(payload))
    }

    @Test
    fun `reply accepts an empty text`() {
        val payload = WearAiCodec.encodeReply("req-1", "")

        assertEquals("req-1" to "", WearAiCodec.decodeReply(payload))
    }

    @Test
    fun `confirm request keeps the summary with line breaks`() {
        val payload = WearAiCodec.encodeConfirmRequest("req-1", "action-9", "Lista\nde teste")

        assertEquals(
            Triple("req-1", "action-9", "Lista\nde teste"),
            WearAiCodec.decodeConfirmRequest(payload)
        )
    }

    @Test
    fun `confirm round trips approved and rejected`() {
        assertEquals(
            Triple("req-1", "action-9", true),
            WearAiCodec.decodeConfirm(WearAiCodec.encodeConfirm("req-1", "action-9", approved = true))
        )
        assertEquals(
            Triple("req-1", "action-9", false),
            WearAiCodec.decodeConfirm(WearAiCodec.encodeConfirm("req-1", "action-9", approved = false))
        )
    }

    @Test
    fun `confirm with an unknown decision is rejected`() {
        val payload = "req-1\naction-9\nmaybe".toByteArray()

        assertNull(WearAiCodec.decodeConfirm(payload))
    }

    @Test
    fun `failure round trips the reason`() {
        val payload = WearAiCodec.encodeFailure("req-1", AiFailure.NO_PROVIDER)

        assertEquals("req-1" to AiFailure.NO_PROVIDER, WearAiCodec.decodeFailure(payload))
    }
}
