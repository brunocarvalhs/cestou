package br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import javax.inject.Inject

internal class ProductProcessMessageUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(message: String): Result<List<Product>> = Result.success(listOf())
}
