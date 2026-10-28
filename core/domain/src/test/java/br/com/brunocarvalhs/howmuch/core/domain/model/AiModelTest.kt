package br.com.brunocarvalhs.howmuch.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiModelTest {

    @Test
    fun `ai model fields are correctly assigned`() {
        val model = AiModel(
            id = "nvidia/nemotron-3-ultra-550b-a55b:free",
            name = "Nemotron 3 Ultra",
            vendor = "NVIDIA",
            provider = "openrouter",
            isFree = true
        )

        assertEquals("nvidia/nemotron-3-ultra-550b-a55b:free", model.id)
        assertEquals("Nemotron 3 Ultra", model.name)
        assertEquals("NVIDIA", model.vendor)
        assertEquals("openrouter", model.provider)
        assertTrue(model.isFree)
    }

    @Test
    fun `vendor and provider are independent fields`() {
        val model = AiModel(id = "x", name = "X", vendor = "NVIDIA", provider = "openrouter")

        assertEquals("NVIDIA", model.vendor)
        assertEquals("openrouter", model.provider)
    }
}
