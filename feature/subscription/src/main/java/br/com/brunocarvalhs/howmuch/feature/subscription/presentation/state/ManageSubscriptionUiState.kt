package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state

import androidx.compose.runtime.Stable

@Stable
data class ManageSubscriptionUiState(
    val formattedPrice: String? = null,
    val isLoadingPrice: Boolean = true
)
