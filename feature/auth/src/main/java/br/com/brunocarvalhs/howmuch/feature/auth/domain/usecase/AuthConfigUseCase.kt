package br.com.brunocarvalhs.howmuch.feature.auth.domain.usecase

import android.content.Context
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class AuthConfigUseCase @Inject constructor(
    @ApplicationContext context: Context,
    private val googleProvider: GoogleProviderUseCase
) {
    private val providers = listOf(googleProvider()).map { it }

    private val configuration = authUIConfiguration {
        this.context = context
        this.providers { providers.forEach { provider(it) } }
    }

    operator fun invoke(): AuthUIConfiguration = configuration
}
