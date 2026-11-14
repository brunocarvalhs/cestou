package br.com.brunocarvalhs.howmuch.core.billing

/**
 * Um plano assinável como o Play o entrega: o preço exibido é o da fase recorrente (nunca o de um
 * trial) e o [offerToken] é o da oferta do base plan, que é o que a compra usa.
 *
 * Público porque `feature:subscription` monta o paywall a partir dele (AGENTS.md §6).
 */
data class PlanOffer(
    val productId: String,
    val basePlanId: String,
    val offerToken: String,
    val formattedPrice: String,
    val billingPeriod: String
)
