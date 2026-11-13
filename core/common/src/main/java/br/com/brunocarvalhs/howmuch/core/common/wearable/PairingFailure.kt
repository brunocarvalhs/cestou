package br.com.brunocarvalhs.howmuch.core.common.wearable

/**
 * Why a pairing session stopped. Shared by the watch, the phone and the analytics `reason` param,
 * so the enum [name] is a wire value: renaming a value is a protocol change.
 */
enum class PairingFailure {
    DISABLED,
    NOT_SIGNED_IN,
    NO_GOOGLE_ACCOUNT,
    ACCOUNT_MISMATCH,
    NOT_PRO,
    PHONE_UNREACHABLE,
    TIMEOUT,
    CANCELED,
    INVALID_KEY,
    SIGN_IN_FAILED;

    companion object {
        /** Returns null for an unknown reason, so an unexpected message cannot crash the screen. */
        fun fromWire(value: String): PairingFailure? = entries.firstOrNull { it.name == value }
    }
}
