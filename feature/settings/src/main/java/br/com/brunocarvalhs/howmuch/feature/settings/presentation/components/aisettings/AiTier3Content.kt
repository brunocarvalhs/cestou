package br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.aisettings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.settings.R

@Composable
internal fun AiTier3Content(
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
