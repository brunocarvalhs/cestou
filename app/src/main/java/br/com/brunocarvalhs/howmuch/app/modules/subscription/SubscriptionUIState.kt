package br.com.brunocarvalhs.howmuch.app.modules.subscription

import br.com.brunocarvalhs.domain.entities.PlanSubscription

sealed interface SubscriptionUIState {
    data object Loading : SubscriptionUIState
    data class Active(
        val plan: PlanSubscription
    ) : SubscriptionUIState
    data class Inactive(
        val list: List<PlanSubscription> = emptyList()
    ) : SubscriptionUIState
}
