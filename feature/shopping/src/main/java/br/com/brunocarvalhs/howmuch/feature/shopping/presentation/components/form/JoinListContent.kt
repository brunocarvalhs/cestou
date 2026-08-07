package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.form

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText

@Composable
internal fun JoinListContent(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
    onScan: () -> Unit,
    error: UiText? = null,
    isLoading: Boolean = false
) {
    var token by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.shopping_management_join_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = stringResource(R.string.shopping_management_join_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = token.uppercase(),
            onValueChange = { 
                if (it.length <= 20) token = it 
            },
            label = { Text(stringResource(R.string.shopping_management_label_token)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.shopping_management_placeholder_token)) },
            singleLine = true,
            isError = error != null,
            supportingText = if (error != null) {
                { Text(error.asString(context), color = MaterialTheme.colorScheme.error) }
            } else null
        )

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = { onJoin(token) },
            enabled = token.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.shopping_management_button_join))
        }

        TextButton(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.shopping_management_button_scan_qr))
        }

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(CoreR.string.action_cancel))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinListContentPreview() {
    MaterialTheme {
        JoinListContent(
            onDismiss = {},
            onJoin = {},
            onScan = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinListContentLoadingPreview() {
    MaterialTheme {
        JoinListContent(
            onDismiss = {},
            onJoin = {},
            onScan = {},
            isLoading = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinListContentErrorPreview() {
    MaterialTheme {
        JoinListContent(
            onDismiss = {},
            onJoin = {},
            onScan = {},
            error = UiText.DynamicString("Token inválido ou expirado")
        )
    }
}
