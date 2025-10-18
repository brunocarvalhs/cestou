package br.com.brunocarvalhs.domain.entities

interface PlanSubscription {
    val id: String
    val name: String
    val price: String
    val offerToken: String
    val renewsAt: String
    val features: List<String>
    val isRecommended: Boolean
}