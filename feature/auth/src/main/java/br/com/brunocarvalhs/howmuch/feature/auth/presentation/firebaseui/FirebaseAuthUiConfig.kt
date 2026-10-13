package br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.firebase.ui.auth.R as FirebaseUiR

internal class FirebaseAuthUiConfig @Inject constructor(
    @ApplicationContext context: Context,
    googleProvider: GoogleAuthProviderFactory,
    featureFlags: FeatureFlagService,
) {
    /** What the picker shows, in order. Resolved once per instance, so a flag change applies on the next launch. */
    val methods: List<AuthMethod> = enabledAuthMethods(featureFlags) {
        it != AuthMethod.FACEBOOK || isFacebookConfigured(context)
    }

    /**
     * Google is always registered, even when its button is hidden: FirebaseUI jumps straight into
     * the only provider when the list is a single Phone entry, which would skip our picker (and
     * the e-mail button). Visibility is decided by [methods], not by this list.
     */
    val providers: List<AuthProvider> = buildList {
        add(googleProvider())
        methods.forEach { method ->
            when (method) {
                AuthMethod.PHONE -> add(
                    AuthProvider.Phone(defaultNumber = null, defaultCountryCode = "BR", allowedCountries = null)
                )
                AuthMethod.APPLE -> add(AuthProvider.Apple(locale = null, customParameters = emptyMap()))
                AuthMethod.MICROSOFT -> add(AuthProvider.Microsoft(tenant = null, customParameters = emptyMap()))
                AuthMethod.FACEBOOK -> add(AuthProvider.Facebook())
                AuthMethod.GITHUB -> add(AuthProvider.Github(customParameters = emptyMap()))
                AuthMethod.GOOGLE, AuthMethod.EMAIL -> Unit
            }
        }
    }

    private val configuration = authUIConfiguration {
        this.context = context
        this.providers { this@FirebaseAuthUiConfig.providers.forEach { provider(it) } }
    }

    operator fun invoke(): AuthUIConfiguration = configuration

    private companion object {
        /** FirebaseUI throws at config time if the SDK or any of the three app values is missing. */
        fun isFacebookConfigured(context: Context): Boolean {
            if (runCatching { Class.forName("com.facebook.login.LoginManager") }.isFailure) return false
            return listOf(
                FirebaseUiR.string.facebook_application_id,
                FirebaseUiR.string.facebook_client_token,
                FirebaseUiR.string.facebook_login_protocol_scheme,
            ).none { id ->
                val value = context.getString(id)
                value.isBlank() || value == FirebaseAuthUI.UNCONFIGURED_CONFIG_VALUE || value == "fb_your_app_id"
            }
        }
    }
}
