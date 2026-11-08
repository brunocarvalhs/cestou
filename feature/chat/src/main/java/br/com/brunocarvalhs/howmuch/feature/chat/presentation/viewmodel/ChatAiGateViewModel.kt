package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Kept out of [AiChatViewModel] on purpose: that class is compiled into `:wear` too (even
 * though nothing there currently calls it), and `:wear` doesn't depend on `core:billing` —
 * adding [SubscriptionRepository] to a class `:wear` might resolve would risk its Hilt graph.
 * Only ever requested from the mobile `chatGraph`.
 *
 * Pro users always get unlimited messages. Free users get exactly one, tracked by
 * [AiTrialRepository] — the AI chat is the Pro differentiator, not any individual agent action.
 */
@HiltViewModel
internal class ChatAiGateViewModel @Inject constructor(
    subscriptionRepository: SubscriptionRepository,
    aiTrialRepository: AiTrialRepository,
    featureFlagService: FeatureFlagService
) : ViewModel() {

    /** Kill-switch dos chips e das ações de adicionar itens (sdd-040). Lido uma vez ao abrir o chat. */
    val isProactiveEnabled: Boolean =
        featureFlagService.isEnabled(FeatureFlagKeys.PROACTIVE_AI_ENABLED, default = true)

    val isSendEnabled: StateFlow<Boolean> = combine(
        subscriptionRepository.observeStatus(),
        aiTrialRepository.hasUsedFreeMessage()
    ) { status, hasUsedFreeMessage ->
        status == SubscriptionStatus.PRO || !hasUsedFreeMessage
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(GATE_TIMEOUT_MS), true)

    private companion object {
        const val GATE_TIMEOUT_MS = 5_000L
    }
}
