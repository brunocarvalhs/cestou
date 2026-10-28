package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.feature.subscription.R

internal data class SubscriptionBenefit(val icon: ImageVector, val textRes: Int)

internal val subscriptionBenefits = listOf(
    SubscriptionBenefit(Icons.AutoMirrored.Filled.Chat, R.string.paywall_benefit_chat),
    SubscriptionBenefit(Icons.Filled.Share, R.string.paywall_benefit_share),
    SubscriptionBenefit(Icons.Filled.QrCode2, R.string.paywall_benefit_qr_code),
    SubscriptionBenefit(Icons.Filled.Watch, R.string.paywall_benefit_wear)
)

/** Shared between [br.com.brunocarvalhs.howmuch.feature.subscription.presentation.screen.PaywallScreen]
 *  (sells the benefits) and the manage-subscription screen (reminds the already-subscribed user of them). */
@Composable
internal fun SubscriptionBenefitsCard(modifier: Modifier = Modifier) {
    CestouCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            subscriptionBenefits.forEachIndexed { index, benefit ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = benefit.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(benefit.textRes),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (index != subscriptionBenefits.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
