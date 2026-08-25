package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.intent.LinkWearIntent
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.state.LinkWearUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LinkWearDeviceScreen(
    state: LinkWearUiState,
    intent: LinkWearIntent,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vincular Relógio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // Icone de voltar omitido por simplicidade ou usar MaterialIcons.AutoMirrored.Filled.ArrowBack
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.isSuccess) {
                Text("Vínculo realizado com sucesso!", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Voltar")
                }
            } else {
                Text(
                    "Digite o código exibido no seu relógio",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.code,
                    onValueChange = intent.onCodeChange,
                    label = { Text("Código de 6 dígitos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.error != null
                )
                if (state.error != null) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = intent.onLinkClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && state.code.length == 6
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Vincular")
                    }
                }
            }
        }
    }
}
