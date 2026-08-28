package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.wear.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.PairingCode
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.wear.viewmodel.PairingViewModel

@Composable
internal fun LinkPhoneWearScreen(
    viewModel: PairingViewModel,
    navigator: Navigator
) {
    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Vincular Celular",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Para começar, abra o app Cestou no seu celular.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.openPhoneApp()
                    navigator.navigate(PairingCode::class.java.name)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir no Celular")
            }
        }
    }
}
