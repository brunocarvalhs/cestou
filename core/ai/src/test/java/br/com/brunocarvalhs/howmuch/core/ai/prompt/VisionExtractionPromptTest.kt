package br.com.brunocarvalhs.howmuch.core.ai.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionExtractionPromptTest {

    @Test
    fun `keeps commercial product names`() {
        val result = VisionExtractionPrompt.sanitizeNames(listOf("Leite Integral", "Arroz Tio João", "Banana"))

        assertEquals(listOf("Leite Integral", "Arroz Tio João", "Banana"), result)
    }

    @Test
    fun `drops nutrition table lines`() {
        val result = VisionExtractionPrompt.sanitizeNames(
            listOf("Valor energético 120 kcal", "Sódio 200mg", "Informação Nutricional", "Café"),
        )

        assertEquals(listOf("Café"), result)
    }

    @Test
    fun `drops codes and random sequences`() {
        val result = VisionExtractionPrompt.sanitizeNames(listOf("1a00", "7891000100103", "L2345", "123", "Feijão"))

        assertEquals(listOf("Feijão"), result)
    }

    @Test
    fun `drops blank, single-character and overlong names`() {
        val result = VisionExtractionPrompt.sanitizeNames(listOf("", "  ", "a", "x".repeat(61), "Pão"))

        assertEquals(listOf("Pão"), result)
    }

    @Test
    fun `removes case-insensitive duplicates keeping the first`() {
        val result = VisionExtractionPrompt.sanitizeNames(listOf("Leite", "leite ", "LEITE", "Ovos"))

        assertEquals(listOf("Leite", "Ovos"), result)
    }

    @Test
    fun `prompt tells the model to ignore nutrition tables and codes`() {
        assertTrue(VisionExtractionPrompt.PROMPT.contains("tabelas nutricionais"))
        assertTrue(VisionExtractionPrompt.PROMPT.contains("1a00"))
        assertFalse(VisionExtractionPrompt.isUsefulName("1a00"))
    }
}
