package br.com.brunocarvalhs.data.model

import br.com.brunocarvalhs.domain.entities.PlanSubscription

data class PlanSubscriptionModel(
    override val id: String,
    override val name: String,
    override val price: String,
    override val offerToken: String,
    override val features: List<String>,
    override val isRecommended: Boolean,
    override val renewsAt: String,
): PlanSubscription