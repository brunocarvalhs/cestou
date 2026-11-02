package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTopBar
import br.com.brunocarvalhs.howmuch.feature.subscription.R
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.components.SubscriptionBenefitsCard
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.intent.ManageSubscriptionIntent
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state.ManageSubscriptionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubscriptionScreen(
    state: ManageSubscriptionUiState,
    intent: ManageSubscriptionIntent
) {
    Scaffold(
        topBar = {
            CestouTopBar(
                title = { Text(stringResource(R.string.paywall_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = intent.onBack,
                        modifier = Modifier.testTag("manage_subscription_back_button")
                    ) {
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = stringResource(R.string.manage_subscription_headline),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            if (state.isLoadingPrice) {
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                state.formattedPrice?.let {
                    Text(
                        text = stringResource(R.string.manage_subscription_price, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 24.dp)
                            .testTag("manage_subscription_price_text")
                    )
                } ?: Spacer(modifier = Modifier.height(24.dp))
            }

            SubscriptionBenefitsCard()

            Spacer(modifier = Modifier.height(24.dp))

            CestouButton(
                text = stringResource(R.string.manage_subscription_play_store_button),
                onClick = intent.onManageOnPlayStore,
                modifier = Modifier.testTag("manage_subscription_manage_button"),
                trailingIcon = Icons.AutoMirrored.Filled.OpenInNew
            )

            Text(
                text = stringResource(R.string.manage_subscription_play_store_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ManageSubscriptionScreenPreview() {
    MaterialTheme {
        ManageSubscriptionScreen(
            state = ManageSubscriptionUiState(formattedPrice = "R$ 9,90", isLoadingPrice = false),
            intent = ManageSubscriptionIntent()
        )
    }
}
