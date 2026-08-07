package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.entity.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import javax.inject.Inject

internal class ProductGetByBarcodeUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(barcode: String): Result<Product?> = repository.getProductByBarcode(barcode)
}
