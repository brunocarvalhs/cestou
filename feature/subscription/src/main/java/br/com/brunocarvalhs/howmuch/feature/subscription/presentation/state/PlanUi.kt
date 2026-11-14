package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state

import androidx.compose.runtime.Stable

/**
 * Um plano exibido no paywall. [basePlanId] identifica o plano (`monthly`, `semiannual`, `annual`)
 * e [formattedPrice] é o preço da fase recorrente, já formatado pelo Play.
 */
@Stable
data class PlanUi(
    val basePlanId: String,
    val formattedPrice: String
)
