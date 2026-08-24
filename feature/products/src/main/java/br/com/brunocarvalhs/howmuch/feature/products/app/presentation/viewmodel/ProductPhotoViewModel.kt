package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductAnalyzeImageUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.commons.navigation.ProductPickerRoute
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent.ProductPhotoIntent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.ProductPhotoUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProductPhotoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val analyzeImageUseCase: ProductAnalyzeImageUseCase,
    private val saveUseCase: ProductSaveUseCase
) : ViewModel() {
    private val shopping = savedStateHandle.toRoute<ProductPickerRoute>(ProductPickerRoute.typeMap).shopping

    private val _uiState = MutableStateFlow(ProductPhotoUiState())
    val uiState = _uiState.asStateFlow()

    val intent = ProductPhotoIntent(
        onImageCaptured = { uri -> onImageCaptured(uri) },
        onRetake = { retake() },
        onAnalyzeImage = { analyzeImage() },
        onProductConfirmed = { product -> onProductConfirmed(product) },
        onAnalysisItemUpdated = { product -> updateAnalysisItem(product) },
        onAnalysisItemRemoved = { id -> removeAnalysisItem(id) },
        onConfirmAllAnalysisItems = { confirmAllAnalysisItems() },
        onConfirmationMessageShown = { onConfirmationMessageShown() }
    )

    private fun onImageCaptured(uri: Uri) {
        _uiState.update { it.copy(capturedImageUri = uri) }
    }

    private fun retake() {
        _uiState.update { it.copy(capturedImageUri = null, analysisResult = emptyList(), errorMessage = null) }
    }

    private fun analyzeImage() {
        val uri = _uiState.value.capturedImageUri ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
            
            val bitmap = loadBitmapFromUri(uri)
            if (bitmap == null) {
                _uiState.update { 
                    it.copy(
                        isAnalyzing = false, 
                        errorMessage = context.getString(R.string.product_error_load_image)
                    ) 
                }
                return@launch
            }

            analyzeImageUseCase(bitmap)
                .onSuccess { products ->
                    _uiState.update { it.copy(isAnalyzing = false, analysisResult = products) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isAnalyzing = false, errorMessage = e.message) }
                }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun onProductConfirmed(product: Product) {
        viewModelScope.launch {
            saveUseCase(product = product, shoppingId = shopping.id)
            _uiState.update { state ->
                state.copy(
                    analysisResult = state.analysisResult.filterNot { it.id == product.id },
                    confirmationMessage = context.getString(R.string.product_photo_item_added, product.name)
                )
            }
        }
    }

    private fun updateAnalysisItem(product: Product) {
        _uiState.update { state ->
            state.copy(analysisResult = state.analysisResult.map { if (it.id == product.id) product else it })
        }
    }

    private fun removeAnalysisItem(id: String) {
        _uiState.update { state ->
            val remaining = state.analysisResult.filterNot { it.id == id }
            state.copy(
                analysisResult = remaining,
                capturedImageUri = if (remaining.isEmpty()) null else state.capturedImageUri
            )
        }
    }

    private fun confirmAllAnalysisItems() {
        val items = _uiState.value.analysisResult
        if (items.isEmpty()) return

        viewModelScope.launch {
            items.forEach { product -> saveUseCase(product = product, shoppingId = shopping.id) }
            _uiState.update {
                it.copy(
                    analysisResult = emptyList(),
                    capturedImageUri = null,
                    confirmationMessage = context.resources.getQuantityString(
                        R.plurals.product_photo_items_added,
                        items.size,
                        items.size
                    )
                )
            }
        }
    }

    private fun onConfirmationMessageShown() {
        _uiState.update { it.copy(confirmationMessage = null) }
    }
}
