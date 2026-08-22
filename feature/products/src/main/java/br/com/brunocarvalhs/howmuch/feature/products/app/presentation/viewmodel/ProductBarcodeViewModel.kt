package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductGetByBarcodeUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.commons.navigation.ProductPickerRoute
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent.ProductBarcodeIntent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.ProductBarcodeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
internal class ProductBarcodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getByBarcodeUseCase: ProductGetByBarcodeUseCase,
    private val saveUseCase: ProductSaveUseCase
) : ViewModel() {
    private val shopping = savedStateHandle.toRoute<ProductPickerRoute>(ProductPickerRoute.typeMap).shopping

    private val _uiState = MutableStateFlow(ProductBarcodeUiState())
    val uiState = _uiState.asStateFlow()

    val intent = ProductBarcodeIntent(
        onBarcodeScanner = { barcode -> onBarcodeScanned(barcode) },
        onFlashToggle = { toggleFlash() }
    )

    private fun toggleFlash() {
        _uiState.update { it.copy(isFlashOn = !it.isFlashOn) }
    }

    private fun onBarcodeScanned(barcode: String) {
        if (_uiState.value.isProcessing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, lastScannedBarcode = barcode) }
            getByBarcodeUseCase(barcode = barcode)
                .onSuccess { product ->
                    product?.let {
                        saveUseCase(
                            product = product,
                            shoppingId = shopping.id
                        )
                    }
                    _uiState.update { it.copy(isProcessing = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isProcessing = false, errorMessage = e.message) }
                }
            
            kotlinx.coroutines.delay(1000.milliseconds)
            _uiState.update { it.copy(isProcessing = false) }
        }
    }
}
