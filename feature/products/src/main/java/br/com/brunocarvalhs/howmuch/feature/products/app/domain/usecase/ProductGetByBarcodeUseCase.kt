package br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import javax.inject.Inject

internal class ProductGetByBarcodeUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(barcode: String): Result<Product?> = repository.getProductByBarcode(barcode)
}
