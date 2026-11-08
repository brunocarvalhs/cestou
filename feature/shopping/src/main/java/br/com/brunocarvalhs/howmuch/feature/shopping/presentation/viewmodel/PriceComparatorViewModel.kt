package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.SupermarketPriceComparison
import br.com.brunocarvalhs.howmuch.core.domain.model.UserLocation
import br.com.brunocarvalhs.howmuch.core.domain.repository.SupermarketRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.CompareSupermarketPricesUseCase
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.PriceComparator
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.PriceComparatorIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.PriceComparatorUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class PriceComparatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productsUseCase: ProductsUseCase,
    private val repository: SupermarketRepository,
    private val compare: CompareSupermarketPricesUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val shopping = savedStateHandle.toRoute<PriceComparator>(PriceComparator.typeMap).shopping

    private val _uiState = MutableStateFlow(PriceComparatorUiState())
    val uiState: StateFlow<PriceComparatorUiState> = _uiState.asStateFlow()

    /** Última (praça, total) já reportada, para não repetir o evento a cada re-emissão da lista. */
    private var lastTracked: Pair<String, Double>? = null

    val intent = PriceComparatorIntent(
        onSaveLocation = ::saveLocation,
        onChangeLocation = {
            analyticsTracker.trackEvent(AnalyticsEvents.PRICE_COMPARATOR_LOCATION_EDIT)
            _uiState.update { it.copy(editingLocation = true) }
        }
    )

    init {
        observe()
    }

    private fun observe() {
        viewModelScope.launch {
            // Abrir o fluxo de produtos também pode falhar, então fica dentro do mesmo tratamento do `catch`.
            flow { emitAll(combine(repository.observeLocation(), productsUseCase(shopping.id), ::Pair)) }
                .catch { error ->
                    Timber.e(error, "Failed to observe the price comparator inputs")
                    showError()
                }
                .collectLatest { (location, products) -> onInputs(location, products) }
        }
    }

    private suspend fun onInputs(location: UserLocation?, products: List<Product>) {
        if (location == null) {
            _uiState.update { it.copy(isLoading = false, location = null, comparison = null, error = null) }
            return
        }
        runCatching { compare(products, location) }
            .onFailure { if (it is CancellationException) throw it }
            .onSuccess { comparison ->
                _uiState.update {
                    it.copy(isLoading = false, location = location, comparison = comparison, error = null)
                }
                track(comparison)
            }
            .onFailure { error ->
                Timber.e(error, "Failed to compare supermarket prices")
                showError()
            }
    }

    private fun showError() {
        _uiState.update {
            it.copy(isLoading = false, error = UiText.StringResource(R.string.price_comparator_error))
        }
    }

    private fun track(comparison: SupermarketPriceComparison) {
        val signature = comparison.location.key to comparison.baseTotal
        if (signature == lastTracked) return
        lastTracked = signature
        analyticsTracker.trackEvent(
            AnalyticsEvents.PRICE_COMPARISON_EXECUTED,
            mapOf(
                AnalyticsParams.STATE to comparison.location.state,
                AnalyticsParams.UNPRICED_COUNT to comparison.unpricedCount,
                AnalyticsParams.SAVINGS to comparison.maxSavings
            )
        )
    }

    private fun saveLocation(state: String, city: String) {
        val location = UserLocation(state = state.trim().uppercase(), city = city.trim())
        if (location.state !in UserLocation.BRAZILIAN_STATES || location.city.isEmpty()) return
        analyticsTracker.trackEvent(
            AnalyticsEvents.PRICE_COMPARATOR_LOCATION_SAVED,
            mapOf(AnalyticsParams.STATE to location.state)
        )
        viewModelScope.launch {
            runCatching { repository.saveLocation(location) }
                .onFailure { if (it is CancellationException) throw it }
                .onSuccess { _uiState.update { state -> state.copy(editingLocation = false) } }
                .onFailure { error ->
                    Timber.e(error, "Failed to save the price comparator location")
                    showError()
                }
        }
    }
}
