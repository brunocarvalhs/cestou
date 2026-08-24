package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent

import android.net.Uri
import br.com.brunocarvalhs.howmuch.core.domain.model.Product

internal data class ProductPhotoIntent(
    val onImageCaptured: (Uri) -> Unit = {},
    val onRetake: () -> Unit = {},
    val onAnalyzeImage: () -> Unit = {},
    val onProductConfirmed: (Product) -> Unit = {},
    val onAnalysisItemUpdated: (Product) -> Unit = {},
    val onAnalysisItemRemoved: (String) -> Unit = {},
    val onConfirmAllAnalysisItems: () -> Unit = {},
    val onConfirmationMessageShown: () -> Unit = {}
)
