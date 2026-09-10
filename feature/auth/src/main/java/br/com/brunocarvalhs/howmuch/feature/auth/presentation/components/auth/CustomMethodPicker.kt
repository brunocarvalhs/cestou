package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.common.extensions.openBrowser
import br.com.brunocarvalhs.howmuch.core.common.util.LegalUrls
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
private fun SocialButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text)
        }
    }
}

@Composable
internal fun CustomMethodPickerTerms(
    onOpenTermsOfUse: (() -> Unit)? = null,
    onOpenPrivacyPolicy: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val openTermsOfUse = onOpenTermsOfUse ?: { context.openBrowser(LegalUrls.TERMS_OF_USE_URL) }
    val openPrivacyPolicy = onOpenPrivacyPolicy ?: { context.openBrowser(LegalUrls.PRIVACY_POLICY_URL) }

    val bodyStyle = MaterialTheme.typography.bodySmall
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = "Ao continuar, você concorda com nossos ", style = bodyStyle, color = bodyColor)
        Text(
            text = "Termos de Uso",
            style = bodyStyle,
            color = linkColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = openTermsOfUse)
        )
        Text(text = " e ", style = bodyStyle, color = bodyColor)
        Text(
            text = "Política de Privacidade",
            style = bodyStyle,
            color = linkColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = openPrivacyPolicy)
        )
        Text(text = ".", style = bodyStyle, color = bodyColor)
    }
}
