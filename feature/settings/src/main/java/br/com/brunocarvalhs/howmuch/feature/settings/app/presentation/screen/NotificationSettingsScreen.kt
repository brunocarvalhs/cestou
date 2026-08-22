package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.components.SettingsHeader
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent.NotificationSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state.NotificationSettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationSettingsScreen(
    state: NotificationSettingsUiState,
    intent: NotificationSettingsIntent
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SettingsHeader(
                title = stringResource(R.string.settings_section_notifications),
                onBack = { intent.onBack() }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_notifications_enable)) },
                supportingContent = { Text(stringResource(R.string.settings_notifications_desc)) },
                trailingContent = {
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { intent.onUpdateNotificationSettings(it, state.reminderTime) }
                    )
                }
            )

            ListItem(
                modifier = Modifier.clickable(enabled = state.notificationsEnabled) { showTimePicker = true },
                headlineContent = { 
                    Text(
                        text = stringResource(R.string.settings_notifications_reminder_time),
                        color = if (state.notificationsEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                },
                supportingContent = { 
                    Text(
                        text = state.reminderTime,
                        color = if (state.notificationsEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }
            )
        }
    }

    if (showTimePicker) {
        val timeParts = state.reminderTime.split(":")
        val timePickerState = rememberTimePickerState(
            initialHour = timeParts[0].toInt(),
            initialMinute = timeParts[1].toInt(),
            is24Hour = true
        )
        val sheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = { showTimePicker = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.action_select_time),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                TimePicker(state = timePickerState)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { showTimePicker = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = {
                            val newTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                            intent.onUpdateNotificationSettings(state.notificationsEnabled, newTime)
                            showTimePicker = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_confirm))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
