package br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Kept out of [ProfileViewModel] on purpose: that class is shared with the Wear OS profile
 * screen (`profileWearGraph`), and `:wear` doesn't depend on `core:billing` — adding
 * [SubscriptionRepository] to `ProfileViewModel`'s constructor would break Wear's Hilt graph.
 * This ViewModel is only ever requested from the mobile `ProfileGraph`.
 */
@HiltViewModel
internal class ProfileSubscriptionViewModel @Inject constructor(
    subscriptionRepository: SubscriptionRepository,
    featureFlagService: FeatureFlagService
) : ViewModel() {

    val status: StateFlow<SubscriptionStatus> = subscriptionRepository.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), SubscriptionStatus.FREE)

    /** Hides the plan card entirely while the paid business rules aren't finalized. */
    val showPlanSection: Boolean =
        featureFlagService.isEnabled(FeatureFlagKeys.BILLING_ENABLED, default = false)

    /** Hides the "pair your watch" entry point while the Wear OS companion app isn't published. */
    val showWearLink: Boolean =
        featureFlagService.isEnabled(FeatureFlagKeys.WEAR_LOGIN_ENABLED, default = false)

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
