package br.com.brunocarvalhs.howmuch.feature.products.presentation.state

import android.net.Uri
import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.entity.Product

@Stable
internal data class ProductPhotoUiState(
    val capturedImageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val analysisResult: List<Product> = emptyList(),
    val errorMessage: String? = null
)
