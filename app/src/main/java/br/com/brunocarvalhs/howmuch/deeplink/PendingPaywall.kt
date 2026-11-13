package br.com.brunocarvalhs.howmuch.deeplink

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.StateFlow

/**
 * A "open plan" request from the watch's deeplink `cestou://subscription`, waiting for the user to
 * be signed in and past the welcome name step, like the other deeplinks. Kept in [SavedStateHandle]
 * so it survives an Activity recreation.
 */
internal class PendingPaywall(private val savedStateHandle: SavedStateHandle) {

    val requested: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_REQUESTED, false)

    fun store() {
        savedStateHandle[KEY_REQUESTED] = true
    }

    fun consume() {
        savedStateHandle[KEY_REQUESTED] = false
    }

    private companion object {
        const val KEY_REQUESTED = "pending_paywall"
    }
}
