package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus

@Stable
data class PaywallUiState(
    val source: String = "",
    val status: SubscriptionStatus = SubscriptionStatus.FREE,
    val formattedPrice: String? = null,
    val isLoadingPrice: Boolean = true,
    val isPurchasing: Boolean = false,
    val errorMessage: String? = null
)
