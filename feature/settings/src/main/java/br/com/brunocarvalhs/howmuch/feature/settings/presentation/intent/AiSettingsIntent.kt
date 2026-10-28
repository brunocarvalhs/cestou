package br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent

import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier

internal data class AiSettingsIntent(
    val onUpdateAiSettings: (AiConfigTier, String, String?, Float, String?) -> Unit = { _, _, _, _, _ -> },
    val onOpenPaywall: () -> Unit = {},
    val onBack: () -> Unit = {}
)
