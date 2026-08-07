package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.entity.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import javax.inject.Inject

internal class ProductProcessMessageUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(message: String): Result<List<Product>> = Result.success(listOf())
}
