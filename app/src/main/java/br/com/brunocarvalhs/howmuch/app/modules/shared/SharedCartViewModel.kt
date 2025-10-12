package br.com.brunocarvalhs.howmuch.app.modules.shared

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.domain.useCases.GetCartUseCase
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.shareText
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toCurrencyString
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedCartViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getCartUseCase: GetCartUseCase,
) : ViewModel() {

    fun onIntent(intent: SharedCartUiIntent) = when (intent) {
        is SharedCartUiIntent.SharedList -> sharedList(intent.id)
        is SharedCartUiIntent.SharedToken -> sharedToken(intent.id)
    }

    private fun sharedList(id: String) = viewModelScope.launch {
        getCartUseCase.invoke(id).onSuccess { cart ->
            context.shareText(
                subject = context.getString(R.string.share_shopping_cart),
                text = buildString {
                    append("Meu carrinho de compras:\n\n")
                    cart.products.forEach { product ->
                        append("${product.name} x${product.quantity} - R$ ${product.price?.toCurrencyString()}\n")
                    }
                    append("\n\nTotal: R$ ${cart.totalPrice.toCurrencyString()}")
                }
            )
        }.onFailure {

        }
    }

    private fun sharedToken(id: String) = viewModelScope.launch {
        getCartUseCase.invoke(id).onSuccess { cart ->
            context.shareText(
                subject = context.getString(R.string.share_cart_token),
                text = buildString {
                    append("🎉 Olha só! Você pode acessar meu carrinho de compras usando este token:\n\n")
                    append(cart.token)
                    append("\n\nBasta inserir o token no app para ver todos os produtos e até adicionar os seus! 🛒")
                }
            )
        }.onFailure {

        }
    }
}