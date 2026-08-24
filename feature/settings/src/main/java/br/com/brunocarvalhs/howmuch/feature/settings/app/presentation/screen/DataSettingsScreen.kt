package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.components.SettingsHeader
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent.DataSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state.DataSettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataSettingsScreen(
    state: DataSettingsUiState,
    intent: DataSettingsIntent
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            intent.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_section_data),
                onBack = { intent.onBack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ListItem(
                modifier = Modifier.clickable { intent.onClearCache() },
                headlineContent = { Text(stringResource(R.string.settings_data_clear_cache)) },
                supportingContent = { Text(stringResource(R.string.settings_data_clear_cache_hint)) }
            )

            ListItem(
                modifier = Modifier.clickable { showDeleteConfirmation = true },
                headlineContent = { 
                    Text(
                        text = stringResource(R.string.settings_data_delete_all),
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                supportingContent = { Text(stringResource(R.string.settings_data_delete_all_hint)) }
            )
        }
    }

    if (showDeleteConfirmation) {
        ModalBottomSheet(
            onDismissRequest = { showDeleteConfirmation = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.settings_data_delete_confirmation_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.settings_data_delete_confirmation_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        intent.onDeleteAllData()
                        showDeleteConfirmation = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.settings_data_delete_button))
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DataSettingsScreenPreview() {
    MaterialTheme {
        DataSettingsScreen(
            state = DataSettingsUiState(),
            intent = DataSettingsIntent()
        )
    }
}
