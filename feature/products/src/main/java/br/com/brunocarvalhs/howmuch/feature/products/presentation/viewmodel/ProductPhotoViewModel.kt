package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

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
import br.com.brunocarvalhs.howmuch.core.domain.entity.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductAnalyzeImageUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductPickerRoute
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.ProductPhotoIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.ProductPhotoUiState
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
        onProductConfirmed = { product -> onProductConfirmed(product) }
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
                _uiState.update { it.copy(isAnalyzing = false, errorMessage = "Erro ao carregar a imagem") }
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
        }
    }
}
