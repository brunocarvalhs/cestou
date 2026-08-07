package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import android.content.Context
import android.content.Intent
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class ShareShoppingUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productsUseCase: ProductsUseCase
) {
    suspend operator fun invoke(shopping: Shopping) {
        val products = productsUseCase(shopping.id).first()
        
        val sb = StringBuilder()
        sb.append("🛒 *${shopping.title}*\n")
        if (shopping.description.isNotBlank()) {
            sb.append("${shopping.description}\n")
        }
        sb.append("\n")
        
        products.forEach { product ->
            val status = if (product.isPurchased) "✅" else "⬜"
            sb.append("$status ${product.name} (${product.quantity}x)\n")
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, shopping.title)
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar lista").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
