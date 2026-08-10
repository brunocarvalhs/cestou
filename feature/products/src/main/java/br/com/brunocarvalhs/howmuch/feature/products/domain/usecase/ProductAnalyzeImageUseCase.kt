package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import android.graphics.Bitmap
import br.com.brunocarvalhs.howmuch.core.domain.entity.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import javax.inject.Inject

internal class ProductAnalyzeImageUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<List<Product>> = repository.analyzeImage(bitmap)
}
