package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.ViewModel
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Kept out of [CartViewModel] on purpose: that class is compiled into `:wear` too, and the AI
 * chat FAB only exists on the mobile cart screen. Only ever requested from the mobile
 * `cartGraph`.
 *
 * Gates the AI chat entry point (AD: new, unproven feature shipped behind a gradual rollout).
 */
@HiltViewModel
internal class CartAiChatGateViewModel @Inject constructor(
    featureFlagService: FeatureFlagService
) : ViewModel() {

    val showAiChatFab: Boolean =
        featureFlagService.isEnabled(FeatureFlagKeys.AI_CHAT_ENABLED, default = false)
}
