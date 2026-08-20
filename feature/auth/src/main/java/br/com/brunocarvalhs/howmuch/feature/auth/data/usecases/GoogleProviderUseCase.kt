package br.com.brunocarvalhs.howmuch.feature.auth.data.usecases

import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import javax.inject.Inject

internal class GoogleProviderUseCase @Inject constructor() {
    operator fun invoke(): AuthProvider = AuthProvider.Google(
        scopes = listOf("email"),
        serverClientId = null,
    )
}
