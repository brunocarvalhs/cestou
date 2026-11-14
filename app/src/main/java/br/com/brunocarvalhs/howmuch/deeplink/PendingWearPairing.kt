package br.com.brunocarvalhs.howmuch.deeplink

import androidx.lifecycle.SavedStateHandle
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.WearPairing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * A watch pairing request from the deeplink, waiting for the user to be signed in (sdd-055).
 * Kept as three [SavedStateHandle] fields so it survives an Activity recreation.
 */
internal class PendingWearPairing(
    private val savedStateHandle: SavedStateHandle,
    scope: CoroutineScope
) {
    val request: StateFlow<WearPairing?> = combine(
        savedStateHandle.getStateFlow<String?>(KEY_SESSION, null),
        savedStateHandle.getStateFlow<String?>(KEY_WATCH_KEY, null),
        savedStateHandle.getStateFlow<String?>(KEY_WATCH_NODE, null)
    ) { sessionId, watchKey, watchNode ->
        if (sessionId != null && watchKey != null && watchNode != null) {
            WearPairing(sessionId = sessionId, wearPublicKey = watchKey, wearNodeId = watchNode)
        } else {
            null
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun store(pairing: WearPairing) {
        savedStateHandle[KEY_SESSION] = pairing.sessionId
        savedStateHandle[KEY_WATCH_KEY] = pairing.wearPublicKey
        savedStateHandle[KEY_WATCH_NODE] = pairing.wearNodeId
    }

    /** Clears the request once the pairing screen has been opened for it. */
    fun consume() {
        savedStateHandle[KEY_SESSION] = null
        savedStateHandle[KEY_WATCH_KEY] = null
        savedStateHandle[KEY_WATCH_NODE] = null
    }

    private companion object {
        const val KEY_SESSION = "pending_wear_session"
        const val KEY_WATCH_KEY = "pending_wear_key"
        const val KEY_WATCH_NODE = "pending_wear_node"
    }
}
