package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.model.AiModel
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.SettingsHeader
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
    // AgentFactoryImpl falls back to the shared app key silently, in the user's name
    // (sdd-017, B-15). Checked right before every exit point instead of in the ViewModel,
    // since both exit points below call onUpdateAiSettings + onBack unconditionally and
    // synchronously, with no way to await the (async) save result first.
    var byokKeyError by remember { mutableStateOf(false) }

    val isPro = state.subscriptionStatus == SubscriptionStatus.PRO

    fun trySave(onSaved: () -> Unit) {
        if (tier == AiConfigTier.CUSTOM_BYOK && customApiKey.isBlank()) {
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
            TierCard(
                title = stringResource(R.string.settings_ai_tier_1_title),
                description = stringResource(R.string.settings_ai_tier_1_desc),
                selected = tier == AiConfigTier.DEFAULT,
                onClick = { tier = AiConfigTier.DEFAULT }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tier 2: Specific Agent
            TierCard(
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
                    Tier2Content(
                        selectedModel = model,
                        availableModels = state.availableModels,
                        onModelSelected = { model = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tier 3: BYOK / Paid API Key
            TierCard(
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
                    Tier3Content(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Tier2Content(
    selectedModel: String,
    availableModels: List<AiModel>,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(8.dp))
    Box(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = showDropdown,
            onExpandedChange = { showDropdown = !showDropdown }
        ) {
            OutlinedTextField(
                value = availableModels.find { it.id == selectedModel }?.name ?: selectedModel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_ai_label_model)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
                modifier = Modifier
                    .menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    )
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false }
            ) {
                availableModels.forEach { aiModel ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(aiModel.name)
                                Text(aiModel.vendor, style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        onClick = {
                            onModelSelected(aiModel.id)
                            showDropdown = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Tier3Content(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = apiKey,
        onValueChange = onApiKeyChange,
        label = { Text(stringResource(R.string.settings_ai_label_custom_api_key)) },
        placeholder = { Text(stringResource(R.string.settings_ai_placeholder_custom_api_key)) },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            {
                Text(
                    text = stringResource(R.string.settings_ai_byok_key_required_error),
                    modifier = Modifier.testTag("ai_settings_byok_key_error")
                )
            }
        } else {
            null
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_settings_byok_key_field")
    )
}

@Composable
private fun TierCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isProRequired: Boolean = false,
    content: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected,
                    onClick = onClick
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (isProRequired) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_ai_pro_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (content != null) {
                content()
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
