package br.com.brunocarvalhs.howmuch.app.modules.editProduct

sealed interface EditProductUiIntent {
    data class Save(
        val name: String,
        val price: Long,
        val quantity: Int,
    ) : EditProductUiIntent
}
