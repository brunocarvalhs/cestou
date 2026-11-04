package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyVisualTransformation
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.SettingsHeader
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.ShoppingSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.ShoppingSettingsUiState

private const val CURRENCY_DIVISOR = 100.0
private const val MAX_GOAL_LENGTH = 12

@Composable
internal fun ShoppingSettingsScreen(
    state: ShoppingSettingsUiState,
    intent: ShoppingSettingsIntent
) {
    var sortingMode by remember(state.sortingMode) {
        mutableStateOf(state.sortingMode)
    }
    var remindersEnabled by remember(state.remindersEnabled) {
        mutableStateOf(state.remindersEnabled)
    }
    var goalDigits by remember(state.monthlyGoal) {
        mutableStateOf(state.monthlyGoal?.let { (it * CURRENCY_DIVISOR).toLong().toString() }.orEmpty())
    }
    val visualTransformation = rememberCurrencyVisualTransformation()
    val currencyFormatter = rememberCurrencyFormatter()
    val currencySymbol = remember(currencyFormatter) { currencyFormatter.currency?.symbol ?: "R$" }

    /** Salva as preferências e a meta (vazia ou zero remove); usado pelo botão e pelo voltar. */
    val saveAll = {
        intent.onUpdateShoppingPreferences(state.defaultListId, sortingMode, remindersEnabled)
        intent.onUpdateMonthlyGoal(goalDigits.toDoubleOrNull()?.div(CURRENCY_DIVISOR))
    }

    Scaffold(
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_section_shopping),
                onBack = {
                    saveAll()
                    intent.onBack()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.settings_shopping_preferences),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = stringResource(R.string.settings_shopping_default_sorting),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = sortingMode == "CATEGORY",
                    onClick = { sortingMode = "CATEGORY" },
                    modifier = Modifier.testTag("shopping_settings_sort_category")
                )
                Text(
                    text = stringResource(R.string.settings_shopping_sort_category),
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                RadioButton(
                    selected = sortingMode == "NAME",
                    onClick = { sortingMode = "NAME" },
                    modifier = Modifier.testTag("shopping_settings_sort_name")
                )
                Text(
                    text = stringResource(R.string.settings_shopping_sort_name),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_shopping_reminders),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_shopping_reminders_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { remindersEnabled = it },
                    modifier = Modifier.testTag("shopping_settings_reminders_switch")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = goalDigits,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        goalDigits = it.take(MAX_GOAL_LENGTH)
                    }
                },
                label = { Text(stringResource(R.string.settings_shopping_monthly_goal)) },
                prefix = { Text(currencySymbol) },
                supportingText = { Text(stringResource(R.string.settings_shopping_monthly_goal_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = visualTransformation,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_monthly_goal_field")
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { saveAll() },
                modifier = Modifier.fillMaxWidth().testTag("shopping_settings_save")
            ) {
                Text(text = stringResource(R.string.settings_ai_button_save))
            }
        }
    }
}

@PreviewCestouScreens
@Composable
private fun ShoppingSettingsScreenCategoryPreview() {
    CestouTheme {
        ShoppingSettingsScreen(
            state = ShoppingSettingsUiState(sortingMode = "CATEGORY", remindersEnabled = false),
            intent = ShoppingSettingsIntent()
        )
    }
}

@PreviewCestouScreens
@Composable
private fun ShoppingSettingsScreenNamePreview() {
    CestouTheme {
        ShoppingSettingsScreen(
            state = ShoppingSettingsUiState(sortingMode = "NAME", remindersEnabled = true),
            intent = ShoppingSettingsIntent()
        )
    }
}
