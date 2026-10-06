package br.com.brunocarvalhs.howmuch.feature.auth.domain.model

/** E-mail/password failures the UI can place on the right field (spec EPA-02..EPA-04). */
internal enum class EmailAuthError {
    /** Wrong password or unknown account: deliberately one value so accounts cannot be enumerated. */
    INVALID_CREDENTIALS,
    INVALID_EMAIL,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    USER_DISABLED,
    TOO_MANY_REQUESTS,
    NETWORK,
    UNKNOWN,
}

internal class EmailAuthException(
    val error: EmailAuthError,
    cause: Throwable? = null,
) : Exception(error.name, cause)
