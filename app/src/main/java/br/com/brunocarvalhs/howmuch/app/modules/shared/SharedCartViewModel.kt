package br.com.brunocarvalhs.howmuch.app.modules.shared

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.domain.usecases.cart.GetCartUseCase
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.shareText
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toCurrencyString
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.routes.SharedCartBottomSheetRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedCartViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val getCartUseCase: GetCartUseCase,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<SharedCartBottomSheetRoute>()

    fun onIntent(intent: SharedCartUiIntent) = when (intent) {
        is SharedCartUiIntent.SharedList -> sharedList(args.cartId)
        is SharedCartUiIntent.SharedToken -> sharedToken(args.cartId)
    }

    private fun sharedList(id: String) = viewModelScope.launch {
        getCartUseCase.invoke(id).onSuccess { cart ->
            context.shareText(
                subject = context.getString(R.string.share_shopping_cart),
                text = buildString {
                    append("Meu carrinho de compras:\n\n")
                    cart.products.forEach { product ->
                        append("${product.name} x${product.quantity}")
                        product.price?.let { append(" - R$ ${product.price?.toCurrencyString()}") }
                        append("\n")
                    }
                    append("\n\nTotal: R$ ${cart.recalculateTotal().toCurrencyString()}")
                }
            )
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
        }
    }
}
