package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.reauth.ReauthContentState

@Composable
internal fun CustomReauthContent(state: ReauthContentState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Confirme sua Identidade",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.reason ?: "Para sua segurança, confirme sua identidade para realizar esta ação.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        state.providers.forEach { provider ->
            CestouButton(
                text = providerLabel(provider),
                onClick = { state.onProviderSelected(provider) },
                enabled = !state.isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = state.onDismiss) {
            Text(text = "Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun providerLabel(provider: AuthProvider): String = when (provider) {
    is AuthProvider.Google -> "Continuar com Google"
    is AuthProvider.Email -> "Entrar com E-mail"
    is AuthProvider.Phone -> "Entrar com Telefone"
    else -> "Entrar com ${provider.providerId}"
}
