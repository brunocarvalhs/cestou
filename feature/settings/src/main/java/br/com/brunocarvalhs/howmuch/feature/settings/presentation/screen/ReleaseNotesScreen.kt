package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.SettingsHeader

@Composable
internal fun ReleaseNotesScreen(
    onBack: () -> Unit
) {
    val notes = listOf(
        "Novidades da Versão 1.0.0",
        "• Lançamento oficial do Cestou!",
        "• Gerenciamento completo de listas de compras.",
        "• Integração com IA para sugestões inteligentes.",
        "• Suporte a múltiplos idiomas e moedas.",
        "• Tema escuro e claro com suporte dinâmico.",
        "• Sincronização em nuvem e compartilhamento de listas."
    )

    Scaffold(
        topBar = {
            SettingsHeader(
                title = "Novidades",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            items(notes) { note ->
                Text(
                    text = note,
                    style = if (note.startsWith("Novidades")) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                if (note.startsWith("Novidades")) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReleaseNotesScreenPreview() {
    MaterialTheme {
        ReleaseNotesScreen(onBack = {})
    }
}
