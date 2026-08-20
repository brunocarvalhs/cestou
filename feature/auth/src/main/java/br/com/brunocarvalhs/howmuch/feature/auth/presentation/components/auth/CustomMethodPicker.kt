package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen.SocialButton
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider

@Composable
internal fun CustomMethodPickerLayout(
    providers: List<AuthProvider>,
    onProviderSelected: (AuthProvider) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        providers.forEach { provider ->
            val (text, icon) = when (provider) {
                is AuthProvider.Google -> "Continuar com Google" to Icons.Default.Email // Replace with real icons later
                is AuthProvider.Apple -> "Continuar com Apple" to Icons.Default.Email
                is AuthProvider.Email -> "Entrar com E-mail" to Icons.Default.Email
                is AuthProvider.Phone -> "Entrar com Telefone" to Icons.Default.Phone
                else -> "Entrar com ${provider.providerId}" to Icons.Default.Email
            }

            SocialButton(
                text = text,
                icon = icon,
                onClick = { onProviderSelected(provider) }
            )
        }
    }
}

@Composable
internal fun CustomMethodPickerTerms() {
    Text(
        text = "Ao continuar, você concorda com nossos Termos de Uso e Política de Privacidade.",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
