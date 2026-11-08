package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state

import androidx.compose.runtime.Immutable
import br.com.brunocarvalhs.howmuch.core.domain.model.SupermarketPriceComparison
import br.com.brunocarvalhs.howmuch.core.domain.model.UserLocation
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText

@Immutable
internal data class PriceComparatorUiState(
    val isLoading: Boolean = true,
    val location: UserLocation? = null,
    val editingLocation: Boolean = false,
    val comparison: SupermarketPriceComparison? = null,
    val error: UiText? = null
) {
    /** Sem localização salva, ou o usuário pediu para trocá-la: mostra o formulário. */
    val showLocationForm: Boolean
        get() = !isLoading && (location == null || editingLocation)
}
