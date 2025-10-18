package br.com.brunocarvalhs.howmuch.app.modules.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.domain.services.SubscriptionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val service: SubscriptionService
) : ViewModel() {

    private val _state = MutableStateFlow<SubscriptionUIState>(SubscriptionUIState.Inactive())
    val state = _state.asStateFlow()

    init {
        fetchPlans()
    }

    fun fetchPlans() = viewModelScope.launch {
        val plans = service.listPlans()
        _state.value = SubscriptionUIState.Inactive(plans)
    }

    fun onIntent(intent: SubscriptionIntent) {
        when (intent) {
            is SubscriptionIntent.Subscribe -> onSubscribe(intent.activity)
            SubscriptionIntent.Cancel -> onCancel()
        }
    }

    fun onSubscribe(activity: Activity) = viewModelScope.launch {
        service.launchPremiumPurchase(activity).onFailure {
            _state.value = SubscriptionUIState.Inactive()
        }
    }

    fun onCancel() {
        _state.value = SubscriptionUIState.Inactive()
    }
}
