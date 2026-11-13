package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear

import androidx.annotation.StringRes
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingFailure
import br.com.brunocarvalhs.howmuch.feature.auth.R

/** The text shown for a pairing failure, on both the watch and the phone. */
@StringRes
internal fun PairingFailure.messageRes(): Int = when (this) {
    PairingFailure.DISABLED -> R.string.wear_pair_failure_disabled
    PairingFailure.NOT_SIGNED_IN -> R.string.wear_pair_failure_not_signed_in
    PairingFailure.NO_GOOGLE_ACCOUNT -> R.string.wear_pair_failure_no_google_account
    PairingFailure.ACCOUNT_MISMATCH -> R.string.wear_pair_failure_account_mismatch
    PairingFailure.NOT_PRO -> R.string.wear_pair_failure_not_pro
    PairingFailure.PHONE_UNREACHABLE -> R.string.wear_pair_failure_phone_unreachable
    PairingFailure.TIMEOUT -> R.string.wear_pair_failure_timeout
    PairingFailure.CANCELED -> R.string.wear_pair_failure_canceled
    PairingFailure.INVALID_KEY -> R.string.wear_pair_failure_invalid_key
    PairingFailure.SIGN_IN_FAILED -> R.string.wear_pair_failure_sign_in_failed
}
