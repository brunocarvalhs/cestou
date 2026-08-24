package br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getSuggestions(shoppingId: String): Flow<List<Product>>
    suspend fun saveProduct(product: Product, shoppingId: String): Result<Unit>
    suspend fun getAllProducts(shoppingId: String): Flow<List<Product>>
    suspend fun deleteProduct(productId: String, shoppingId: String): Result<Unit>
    suspend fun updateProduct(product: Product, shoppingId: String): Result<Unit>
    suspend fun searchProducts(query: String): Result<List<Product>>
    fun getQuestionSuggestions(shoppingId: String): Flow<List<String>>
    suspend fun analyzeImage(bitmap: android.graphics.Bitmap): Result<List<Product>>
}
