package br.com.brunocarvalhs.howmuch.feature.auth.app.domain.usecase

import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import javax.inject.Inject

internal class GoogleProviderUseCase @Inject constructor() {
    operator fun invoke(): AuthProvider = AuthProvider.Google(
        scopes = listOf("email"),
        serverClientId = "801865394084-53u2hd6kg2vvdsf1jm63l3ofh4gc45ai.apps.googleusercontent.com",
    )
}
