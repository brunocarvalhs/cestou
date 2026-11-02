package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier

/**
 * Blocks leaving the screen with tier=CUSTOM_BYOK and a blank key: without this,
 * AgentFactoryImpl falls back to the shared app key silently, in the user's name
 * (sdd-017, B-15). Checked right before every exit point instead of in the ViewModel,
 * since both exit points call onUpdateAiSettings + onBack unconditionally and
 * synchronously, with no way to await the (async) save result first.
 */
internal fun validateByokSave(tier: AiConfigTier, customApiKey: String): Boolean =
    !(tier == AiConfigTier.CUSTOM_BYOK && customApiKey.isBlank())
