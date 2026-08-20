package br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.WelcomeIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.WelcomeUiState

@Composable
internal fun WelcomeScreen(
    state: WelcomeUiState,
    intent: WelcomeIntent
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Logo and Name
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Placeholder for Logo
                Image(
                    painter = painterResource(id = android.R.drawable.ic_menu_gallery), // Mock
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Cestou",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main Illustration (Mock)
            Image(
                painter = painterResource(id = android.R.drawable.ic_dialog_map), // Mock
                contentDescription = null,
                modifier = Modifier.size(280.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Text Content
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.welcome_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Action Button
            CestouButton(
                text = stringResource(R.string.welcome_button),
                onClick = { intent.onStart() },
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = stringResource(R.string.welcome_footer, state.version),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview
@Composable
private fun WelcomeScreenPreview() {
    WelcomeScreen(
        state = WelcomeUiState(),
        intent = WelcomeIntent()
    )
}
