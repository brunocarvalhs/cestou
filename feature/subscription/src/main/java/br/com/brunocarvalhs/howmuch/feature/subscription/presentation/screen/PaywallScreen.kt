package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.screen

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.billing.BillingConstants
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTopBar
import br.com.brunocarvalhs.howmuch.feature.subscription.R
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.components.SubscriptionBenefitsCard
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.intent.PaywallIntent
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state.PaywallUiState
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state.PlanUi

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
                    IconButton(onClick = intent.onBack, modifier = Modifier.testTag("paywall_back")) {
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

            SubscriptionBenefitsCard()

            Spacer(modifier = Modifier.height(24.dp))

            state.errorMessage?.let { message ->
                Text(
                    text = stringResource(R.string.paywall_error_prefix, message),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (state.isLoadingPlans) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
            } else {
                // Com um só plano (flag desligada ou planos ainda não ativos no Play), a lista some e
                // sobra só o botão, como antes.
                if (state.plans.size > 1) {
                    state.plans.forEach { plan ->
                        PlanOptionCard(
                            plan = plan,
                            selected = plan.basePlanId == state.selectedPlanId,
                            onSelect = { intent.onPlanSelected(plan.basePlanId) },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                CestouButton(
                    text = state.selectedPlan
                        ?.let { stringResource(R.string.paywall_subscribe_button, it.formattedPrice) }
                        ?: stringResource(R.string.paywall_subscribe_button_no_price),
                    onClick = { activity?.let(intent.onSubscribeClick) },
                    modifier = Modifier.testTag("paywall_subscribe_button"),
                    enabled = !state.isPurchasing && activity != null
                )
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    plan: PlanUi,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = planLabels(plan.basePlanId) ?: return
    CestouCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("paywall_plan_${plan.basePlanId}"),
        onClick = onSelect,
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = stringResource(labels.name),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(labels.price, plan.formattedPrice),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private data class PlanLabels(@StringRes val name: Int, @StringRes val price: Int)

/** Textos de cada plano conhecido. `null` para um base plan que o app não sabe exibir. */
private fun planLabels(basePlanId: String): PlanLabels? = when (basePlanId) {
    BillingConstants.BASE_PLAN_MONTHLY -> PlanLabels(
        R.string.paywall_plan_monthly_name,
        R.string.paywall_plan_monthly_price
    )
    BillingConstants.BASE_PLAN_SEMIANNUAL -> PlanLabels(
        R.string.paywall_plan_semiannual_name,
        R.string.paywall_plan_semiannual_price
    )
    BillingConstants.BASE_PLAN_ANNUAL -> PlanLabels(
        R.string.paywall_plan_annual_name,
        R.string.paywall_plan_annual_price
    )
    else -> null
}

@Preview(showBackground = true)
@Composable
private fun PaywallScreenPreview() {
    MaterialTheme {
        PaywallScreen(
            state = PaywallUiState(
                plans = listOf(PlanUi("monthly", "R$ 9,90"), PlanUi("annual", "R$ 99,90")),
                selectedPlanId = "annual",
                isLoadingPlans = false
            ),
            intent = PaywallIntent(),
            activity = null
        )
    }
}
