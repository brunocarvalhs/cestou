package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus

@Stable
data class PaywallUiState(
    val source: String = "",
    val status: SubscriptionStatus = SubscriptionStatus.FREE,
    val plans: List<PlanUi> = emptyList(),
    val selectedPlanId: String? = null,
    val isLoadingPlans: Boolean = true,
    val isPurchasing: Boolean = false,
    val errorMessage: String? = null
) {
    /** O plano selecionado, ou `null` quando nenhum plano foi carregado do Play. */
    val selectedPlan: PlanUi?
        get() = plans.firstOrNull { it.basePlanId == selectedPlanId }
}
