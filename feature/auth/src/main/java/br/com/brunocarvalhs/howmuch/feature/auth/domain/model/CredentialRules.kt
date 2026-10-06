package br.com.brunocarvalhs.howmuch.feature.auth.domain.model

/** Client-side checks run before calling Firebase, so errors land on the field (spec EPA-02..04). */
internal object CredentialRules {
    const val MIN_PASSWORD_LENGTH = 8

    // Shape check only; Firebase is the authority. android.util.Patterns is avoided to stay JVM-testable.
    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

    fun isValidEmail(email: String): Boolean = EMAIL.matches(email.trim())
}
