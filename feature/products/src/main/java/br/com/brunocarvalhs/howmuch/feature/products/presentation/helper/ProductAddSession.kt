package br.com.brunocarvalhs.howmuch.feature.products.presentation.helper

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sabe se o seletor de produto fechou sem nenhum produto adicionado, por qualquer caminho
 * (formulário ou foto). Cada ViewModel do seletor abre e fecha a mesma sessão, então só a
 * primeira a ser limpa decide se foi abandono.
 */
@Singleton
internal class ProductAddSession @Inject constructor() {
    private var open = false
    private var added = false

    fun onOpened() {
        open = true
    }

    fun onProductAdded() {
        added = true
    }

    /** `true` uma única vez por abertura, e só se nada foi adicionado. */
    fun onClosedAbandoned(): Boolean {
        if (!open) return false
        val abandoned = !added
        open = false
        added = false
        return abandoned
    }
}
