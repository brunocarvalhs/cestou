package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Kept out of [CartViewModel] on purpose: that class is compiled into `:wear` too (even though
 * nothing there currently calls it), and `:wear` doesn't depend on `core:billing` — adding
 * [SubscriptionRepository] to a class `:wear` might resolve would risk its Hilt graph. Only
 * ever requested from the mobile `cartGraph`.
 *
 * Gates sharing a list (AD-010: sharing is a Pro feature, no free trial).
 */
@HiltViewModel
internal class CartSubscriptionGateViewModel @Inject constructor(
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val isPro: StateFlow<Boolean> = subscriptionRepository.observeStatus()
        .map { it == SubscriptionStatus.PRO }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(GATE_TIMEOUT_MS), false)

    private companion object {
        const val GATE_TIMEOUT_MS = 5_000L
    }
}
