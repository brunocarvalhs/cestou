package br.com.brunocarvalhs.howmuch.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiModelTest {

    @Test
    fun `every free model routes through openrouter, since none is hosted via the Gemini SDK`() {
        AiModel.freeModels.forEach { model ->
            assertEquals("model '${model.id}' should route via openrouter", "openrouter", model.provider)
        }
    }

    @Test
    fun `every free model has a non-blank vendor label for the UI`() {
        AiModel.freeModels.forEach { model ->
            assertTrue("model '${model.id}' should have a vendor label", model.vendor.isNotBlank())
        }
    }

    @Test
    fun `vendor and provider are independent fields`() {
        val model = AiModel(id = "x", name = "X", vendor = "NVIDIA", provider = "openrouter")

        assertEquals("NVIDIA", model.vendor)
        assertEquals("openrouter", model.provider)
    }
}
