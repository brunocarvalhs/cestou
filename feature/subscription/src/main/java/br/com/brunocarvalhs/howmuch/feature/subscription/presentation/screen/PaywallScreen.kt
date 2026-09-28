package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.screen

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTopBar
import br.com.brunocarvalhs.howmuch.feature.subscription.R
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.intent.PaywallIntent
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state.PaywallUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    state: PaywallUiState,
    intent: PaywallIntent,
    activity: Activity?
) {
    Scaffold(
        topBar = {
            CestouTopBar(
                title = { Text(stringResource(R.string.paywall_title)) },
                navigationIcon = {
                    IconButton(onClick = intent.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.paywall_back_content_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = stringResource(R.string.paywall_headline),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.paywall_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            if (state.status == SubscriptionStatus.PRO) {
                CestouCard {
                    Text(
                        text = stringResource(R.string.paywall_already_pro),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                return@Scaffold
            }

            state.errorMessage?.let { message ->
                Text(
                    text = stringResource(R.string.paywall_error_prefix, message),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (state.isLoadingPrice) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
            } else {
                CestouButton(
                    text = state.formattedPrice
                        ?.let { stringResource(R.string.paywall_subscribe_button, it) }
                        ?: stringResource(R.string.paywall_subscribe_button_no_price),
                    onClick = { activity?.let(intent.onSubscribeClick) },
                    enabled = !state.isPurchasing && activity != null
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaywallScreenPreview() {
    MaterialTheme {
        PaywallScreen(
            state = PaywallUiState(formattedPrice = "R$ 9,90", isLoadingPrice = false),
            intent = PaywallIntent(),
            activity = null
        )
    }
}
