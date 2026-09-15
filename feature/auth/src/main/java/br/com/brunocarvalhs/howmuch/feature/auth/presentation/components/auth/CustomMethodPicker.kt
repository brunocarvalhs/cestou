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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.common.extensions.openBrowser
import br.com.brunocarvalhs.howmuch.core.common.util.LegalUrls
import br.com.brunocarvalhs.howmuch.feature.auth.R
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreUiR

@Composable
internal fun CustomMethodPickerLayout(
    providers: List<AuthProvider>,
    onProviderSelected: (AuthProvider) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        providers.forEach { provider ->
            val (text, icon) = when (provider) {
                is AuthProvider.Google -> stringResource(R.string.auth_continue_with_google) to providerIcon {
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_google),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }

                is AuthProvider.Email -> stringResource(R.string.auth_continue_with_email) to providerIcon {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                }

                is AuthProvider.Phone -> stringResource(R.string.auth_continue_with_phone) to providerIcon {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null)
                }

                else -> stringResource(R.string.auth_continue_with_provider, provider.providerId) to providerIcon {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                }
            }
            val testTag = when (provider) {
                is AuthProvider.Google -> "welcome_google_button"
                is AuthProvider.Email -> "welcome_email_button"
                is AuthProvider.Phone -> "welcome_phone_button"
                else -> null
            }

            SocialButton(
                text = text,
                icon = icon,
                onClick = { onProviderSelected(provider) },
                modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier
            )
        }
    }
}

private fun providerIcon(icon: @Composable () -> Unit): @Composable () -> Unit = icon

@Composable
private fun SocialButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, style = MaterialTheme.typography.titleMedium)
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
        Text(text = stringResource(R.string.auth_terms_prefix), style = bodyStyle, color = bodyColor)
        Text(
            text = stringResource(R.string.auth_terms_of_use),
            style = bodyStyle,
            color = linkColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .testTag("welcome_terms_of_use_link")
                .clickable(onClick = openTermsOfUse)
        )
        Text(text = stringResource(R.string.auth_terms_and), style = bodyStyle, color = bodyColor)
        Text(
            text = stringResource(R.string.auth_privacy_policy),
            style = bodyStyle,
            color = linkColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .testTag("welcome_privacy_policy_link")
                .clickable(onClick = openPrivacyPolicy)
        )
        Text(text = stringResource(R.string.auth_terms_suffix), style = bodyStyle, color = bodyColor)
    }
}
