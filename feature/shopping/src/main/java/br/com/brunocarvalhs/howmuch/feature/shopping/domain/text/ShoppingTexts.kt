package br.com.brunocarvalhs.howmuch.feature.shopping.domain.text

/**
 * User-facing texts the shopping use cases need outside any screen (AI agent actions,
 * notifications sent to other members). Resolved in `data` from string resources, so the
 * domain stays free of Android.
 */
interface ShoppingTexts {
    fun newListTitle(): String
    fun newListDescription(): String
    fun copySuffix(): String
    fun listJoinedTitle(): String
    fun listJoinedMessage(actorName: String?, listTitle: String): String
}
