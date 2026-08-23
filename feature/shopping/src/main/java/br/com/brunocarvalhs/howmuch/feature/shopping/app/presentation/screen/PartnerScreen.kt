package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTextField
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent.PartnerIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state.PartnerUiState

@Composable
internal fun PartnerScreen(
    state: PartnerUiState,
    intent: PartnerIntent
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Parceiro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state.partner != null) {
                Text(text = "Você está vinculado com:", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = state.partner.name ?: "Usuário",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                CestouButton(
                    text = "Desvincular",
                    onClick = { intent.onUnlinkPartner() },
                    containerColor = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "Vincule-se a um parceiro para compartilhar suas listas em tempo real.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                CestouTextField(
                    value = "",
                    onValueChange = {},
                    label = "ID do Parceiro",
                    placeholder = "Cole o ID aqui"
                )
                Spacer(modifier = Modifier.height(16.dp))
                CestouButton(
                    text = "Vincular Parceiro",
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Sem parceiro")
@Composable
private fun PartnerScreenUnlinkedPreview() {
    MaterialTheme {
        PartnerScreen(
            state = PartnerUiState(),
            intent = PartnerIntent()
        )
    }
}

@Preview(showBackground = true, name = "Com parceiro")
@Composable
private fun PartnerScreenLinkedPreview() {
    MaterialTheme {
        PartnerScreen(
            state = PartnerUiState(
                partner = UserProfile(id = "1", name = "João Souza", email = "joao@example.com")
            ),
            intent = PartnerIntent()
        )
    }
}
