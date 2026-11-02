package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.model.AiModel
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.SettingsHeader
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.aisettings.AiTier2Content
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.aisettings.AiTier3Content
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.aisettings.AiTierCard
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.AiSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.AiSettingsUiState

@Composable
internal fun AiSettingsScreen(
    state: AiSettingsUiState,
    intent: AiSettingsIntent,
    modifier: Modifier = Modifier
) {
    var tier by remember(state.aiConfigTier) { mutableStateOf(state.aiConfigTier) }
    var model by remember(state.aiModel) { mutableStateOf(state.aiModel) }
    var customApiKey by remember(state.customApiKey) { mutableStateOf(state.customApiKey) }
    var creativity by remember(state.creativityLevel) { mutableFloatStateOf(state.creativityLevel) }
    // Blocks leaving the screen with tier=CUSTOM_BYOK and a blank key: without this,
    // AiAgentFactoryImpl falls back to the shared app key silently, in the user's name
    // (sdd-017, B-15). Checked right before every exit point instead of in the ViewModel,
    // since both exit points below call onUpdateAiSettings + onBack unconditionally and
    // synchronously, with no way to await the (async) save result first.
    var byokKeyError by remember { mutableStateOf(false) }

    val isPro = state.subscriptionStatus == SubscriptionStatus.PRO

    fun trySave(onSaved: () -> Unit) {
        if (!validateByokSave(tier, customApiKey)) {
            byokKeyError = true
            return
        }
        intent.onUpdateAiSettings(tier, model, creativity, customApiKey.ifEmpty { null })
        onSaved()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_section_ai),
                onBack = { trySave(onSaved = intent.onBack) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.settings_ai_header),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tier 1: Default
            AiTierCard(
                title = stringResource(R.string.settings_ai_tier_1_title),
                description = stringResource(R.string.settings_ai_tier_1_desc),
                selected = tier == AiConfigTier.DEFAULT,
                onClick = { tier = AiConfigTier.DEFAULT }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tier 2: Specific Agent
            AiTierCard(
                title = stringResource(R.string.settings_ai_tier_2_title),
                description = stringResource(R.string.settings_ai_tier_2_desc),
                selected = tier == AiConfigTier.SPECIFIC,
                isProRequired = !isPro,
                onClick = {
                    if (isPro) {
                        tier = AiConfigTier.SPECIFIC
                    } else {
                        intent.onOpenPaywall()
                    }
                }
            ) {
                if (tier == AiConfigTier.SPECIFIC && isPro) {
                    AiTier2Content(
                        selectedModel = model,
                        availableModels = state.availableModels,
                        onModelSelected = { model = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tier 3: BYOK / Paid API Key
            AiTierCard(
                title = stringResource(R.string.settings_ai_tier_3_title),
                description = stringResource(R.string.settings_ai_tier_3_desc),
                selected = tier == AiConfigTier.CUSTOM_BYOK,
                isProRequired = !isPro,
                onClick = {
                    if (isPro) {
                        tier = AiConfigTier.CUSTOM_BYOK
                    } else {
                        intent.onOpenPaywall()
                    }
                }
            ) {
                if (tier == AiConfigTier.CUSTOM_BYOK && isPro) {
                    AiTier3Content(
                        apiKey = customApiKey,
                        onApiKeyChange = {
                            customApiKey = it
                            byokKeyError = false
                        },
                        isError = byokKeyError
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_ai_label_creativity, creativity),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Slider(
                value = creativity,
                onValueChange = { creativity = it },
                valueRange = 0f..1f,
                steps = 10,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = { trySave(onSaved = intent.onBack) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_settings_save_button")
            ) {
                Text(text = stringResource(R.string.settings_ai_button_save))
            }
        }
    }
}

@PreviewCestouScreens
@Composable
private fun AiSettingsScreenDefaultPreview() {
    CestouTheme {
        AiSettingsScreen(
            state = AiSettingsUiState(),
            intent = AiSettingsIntent()
        )
    }
}

@PreviewCestouScreens
@Composable
private fun AiSettingsScreenProPreview() {
    CestouTheme {
        AiSettingsScreen(
            state = AiSettingsUiState(
                aiConfigTier = AiConfigTier.SPECIFIC,
                aiModel = "openrouter/free",
                availableModels = listOf(
                    AiModel("openrouter/free", "OpenRouter Free Router", vendor = "OpenRouter")
                ),
                subscriptionStatus = SubscriptionStatus.PRO
            ),
            intent = AiSettingsIntent()
        )
    }
}
