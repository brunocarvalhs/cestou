package br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui

import android.content.Context
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class FirebaseAuthUiConfig @Inject constructor(
    @ApplicationContext context: Context,
    private val googleProvider: GoogleAuthProviderFactory
) {
    private val providers = listOf(googleProvider()).map { it }

    private val configuration = authUIConfiguration {
        this.context = context
        this.providers { providers.forEach { provider(it) } }
    }

    operator fun invoke(): AuthUIConfiguration = configuration
}
