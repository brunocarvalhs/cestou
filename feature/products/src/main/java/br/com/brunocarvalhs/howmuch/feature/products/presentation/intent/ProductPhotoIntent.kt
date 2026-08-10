package br.com.brunocarvalhs.howmuch.feature.products.presentation.intent

import android.net.Uri
import br.com.brunocarvalhs.howmuch.core.domain.entity.Product

internal data class ProductPhotoIntent(
    val onImageCaptured: (Uri) -> Unit = {},
    val onRetake: () -> Unit = {},
    val onAnalyzeImage: () -> Unit = {},
    val onProductConfirmed: (Product) -> Unit = {}
)
