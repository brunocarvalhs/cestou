package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.SettingsHeader

@Composable
internal fun LegalContentScreen(
    title: String,
    content: String,
    url: String,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            SettingsHeader(
                title = title,
                onBack = onBack
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
            TextButton(
                onClick = { onOpenUrl(url) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.settings_legal_view_online),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LegalContentScreenPreview() {
    MaterialTheme {
        LegalContentScreen(
            title = "Política de Privacidade",
            content = "Esta política descreve como coletamos, usamos e protegemos " +
                "os seus dados pessoais ao utilizar o Cestou. Ao continuar usando o " +
                "aplicativo, você concorda com os termos descritos aqui.",
            url = "https://bruno-carvalho.dev.br/legal?doc=cestou-privacy-policy",
            onOpenUrl = {},
            onBack = {}
        )
    }
}
