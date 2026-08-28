package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.wear.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.PairingCode
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.wear.state.PairingUiState
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.wear.viewmodel.PairingViewModel

@Composable
internal fun PairingCodeWearScreen(
    viewModel: PairingViewModel,
    navigator: Navigator
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isLinked) {
        if (state.isLinked) {
            navigator.navigate(ShoppingList::class.java.name) {
                popUpTo(PairingCode::class.java.name) { inclusive = true }
            }
        }
    }

    PairingCodeWearContent(state = state)
}

@Composable
private fun PairingCodeWearContent(state: PairingUiState) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLinked) {
                Text("Vinculado!", style = MaterialTheme.typography.titleMedium)
            } else {
                Text(
                    "Código de Vínculo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.code.chunked(3).joinToString(" "),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Insira este código no app do seu celular",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
