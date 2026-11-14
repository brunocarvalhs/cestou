package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state

import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure

/** Where the watch is in one pairing session. */
internal enum class PairingStage { IDLE, CONNECTING, CODE, SYNCING, ERROR }

internal data class PairingUiState(
    val stage: PairingStage = PairingStage.IDLE,
    /** `XXXX XXXX`, the same on both devices. Empty until the phone answers. */
    val code: String = "",
    /** The watch user tapped "Parear". */
    val isWatchConfirmed: Boolean = false,
    /** Set only when [stage] is [PairingStage.ERROR]. */
    val failure: PairingFailure? = null,
    val isLinked: Boolean = false
)
