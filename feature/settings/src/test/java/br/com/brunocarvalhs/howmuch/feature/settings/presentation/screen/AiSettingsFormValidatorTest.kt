package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSettingsFormValidatorTest {

    @Test
    fun `blocks save when tier is BYOK and key is blank`() {
        assertFalse(validateByokSave(AiConfigTier.CUSTOM_BYOK, ""))
    }

    @Test
    fun `blocks save when tier is BYOK and key is only whitespace`() {
        assertFalse(validateByokSave(AiConfigTier.CUSTOM_BYOK, "   "))
    }

    @Test
    fun `allows save when tier is BYOK and key is present`() {
        assertTrue(validateByokSave(AiConfigTier.CUSTOM_BYOK, "sk-test-key"))
    }

    @Test
    fun `allows save when tier is DEFAULT regardless of key`() {
        assertTrue(validateByokSave(AiConfigTier.DEFAULT, ""))
    }

    @Test
    fun `allows save when tier is SPECIFIC regardless of key`() {
        assertTrue(validateByokSave(AiConfigTier.SPECIFIC, ""))
    }
}
