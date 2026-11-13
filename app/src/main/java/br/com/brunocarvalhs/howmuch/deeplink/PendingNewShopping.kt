package br.com.brunocarvalhs.howmuch.deeplink

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.StateFlow

/**
 * A "create list" request from the watch's deeplink `cestou://shopping/new`, waiting for the user to
 * be signed in (sdd-056). Kept in [SavedStateHandle] so it survives an Activity recreation.
 */
internal class PendingNewShopping(private val savedStateHandle: SavedStateHandle) {

    val requested: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_REQUESTED, false)

    fun store() {
        savedStateHandle[KEY_REQUESTED] = true
    }

    fun consume() {
        savedStateHandle[KEY_REQUESTED] = false
    }

    private companion object {
        const val KEY_REQUESTED = "pending_new_shopping"
    }
}
