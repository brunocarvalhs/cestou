package br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui

import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys

/**
 * A way to sign in, switched on/off remotely. The declaration order is the order of the buttons.
 * [providerId] is the FirebaseUI provider id; null for e-mail, which is our own flow.
 */
internal enum class AuthMethod(val flagKey: String, val defaultEnabled: Boolean, val providerId: String?) {
    GOOGLE(FeatureFlagKeys.AUTH_GOOGLE_ENABLED, true, "google.com"),
    EMAIL(FeatureFlagKeys.AUTH_EMAIL_ENABLED, true, null),
    PHONE(FeatureFlagKeys.AUTH_PHONE_ENABLED, false, "phone"),
    APPLE(FeatureFlagKeys.AUTH_APPLE_ENABLED, false, "apple.com"),
    MICROSOFT(FeatureFlagKeys.AUTH_MICROSOFT_ENABLED, false, "microsoft.com"),
    FACEBOOK(FeatureFlagKeys.AUTH_FACEBOOK_ENABLED, false, "facebook.com"),
    GITHUB(FeatureFlagKeys.AUTH_GITHUB_ENABLED, false, "github.com"),
}

/**
 * The methods to offer. Never empty: when every flag is off, falls back to Google so the user
 * always has a way in. [isAvailable] lets a method be dropped even when its flag is on (e.g.
 * Facebook without its app id configured, which would crash FirebaseUI).
 */
internal fun enabledAuthMethods(
    flags: FeatureFlagService,
    isAvailable: (AuthMethod) -> Boolean = { true },
): List<AuthMethod> =
    AuthMethod.entries
        .filter { flags.isEnabled(it.flagKey, it.defaultEnabled) && isAvailable(it) }
        .ifEmpty { listOf(AuthMethod.GOOGLE) }
