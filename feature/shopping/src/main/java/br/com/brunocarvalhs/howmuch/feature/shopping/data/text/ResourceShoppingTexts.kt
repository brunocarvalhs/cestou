package br.com.brunocarvalhs.howmuch.feature.shopping.data.text

import android.content.Context
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.text.ShoppingTexts
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

internal class ResourceShoppingTexts @Inject constructor(
    @ApplicationContext private val context: Context
) : ShoppingTexts {
    override fun newListTitle(): String = context.getString(R.string.shopping_list_new_title)
    override fun newListDescription(): String = context.getString(R.string.shopping_list_new_description)
    override fun copySuffix(): String = context.getString(R.string.shopping_list_copy_suffix)
    override fun listJoinedTitle(): String = context.getString(CoreR.string.notification_list_joined_title)
    override fun listJoinedMessage(actorName: String?, listTitle: String): String = context.getString(
        CoreR.string.notification_list_joined_message,
        actorName ?: context.getString(CoreR.string.notification_someone),
        listTitle
    )
}
