package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import javax.inject.Inject

class ProductAnalyzeImageUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(imageUri: String): Result<List<Product>> = repository.analyzeImage(imageUri)
}
