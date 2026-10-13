package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Picks the list the watch opens straight into: the last one opened, else the newest unfinished one. */
@HiltViewModel
internal class LastListViewModel @Inject constructor(
    private val wearListStore: WearListStore,
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    suspend fun lastListId(): String? =
        wearListStore.lastList()?.shoppingId
            ?: shoppingRepository.getAll()
                .filter { it.status != Shopping.Status.FINISH }
                .maxByOrNull { it.createdAt }
                ?.id
}
