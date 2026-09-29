package br.com.brunocarvalhs.howmuch.feature.products.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.repository.ProductReader
import kotlinx.coroutines.flow.Flow

// AD-011: ProductReader (core/domain) covers getAllProducts, the one capability external
// consumers need; ProductRepository is the feature-owned superset that adds writes.
interface ProductRepository : ProductReader {
    fun getSuggestions(shoppingId: String): Flow<List<Product>>
    suspend fun saveProduct(product: Product, shoppingId: String): Result<Unit>
    suspend fun deleteProduct(productId: String, shoppingId: String): Result<Unit>
    suspend fun updateProduct(product: Product, shoppingId: String): Result<Unit>
    suspend fun searchProducts(query: String): Result<List<Product>>
    fun getQuestionSuggestions(shoppingId: String): Flow<List<String>>
    suspend fun analyzeImage(bitmap: android.graphics.Bitmap): Result<List<Product>>
}
