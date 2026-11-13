package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state

import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure

/** Where the phone is in one pairing session. */
internal enum class PhoneWearPairingStage { CONNECTING, CODE, SYNCING, DONE, ERROR }

internal data class PhoneWearPairingUiState(
    val stage: PhoneWearPairingStage = PhoneWearPairingStage.CONNECTING,
    /** `XXXX XXXX`, the same code the watch shows. Empty until it is derived. */
    val code: String = "",
    /** The phone user tapped "Parear". */
    val isPhoneConfirmed: Boolean = false,
    /** The watch user tapped "Parear" (received over the wearable channel). */
    val isWatchConfirmed: Boolean = false,
    /** Set only when [stage] is [PhoneWearPairingStage.ERROR]. */
    val failure: PairingFailure? = null
)
