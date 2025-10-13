package br.com.brunocarvalhs.howmuch.app.modules.editProduct

sealed interface EditProductUiIntent {
    data class Save(val price: Long) : EditProductUiIntent
}
