package br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui

import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import javax.inject.Inject

internal class GoogleAuthProviderFactory @Inject constructor() {
    operator fun invoke(): AuthProvider = AuthProvider.Google(
        scopes = listOf("email"),
        serverClientId = SERVER_CLIENT_ID,
    )

    companion object {
        /** Web client id (client_type 3 in google-services.json). */
        const val SERVER_CLIENT_ID = "801865394084-53u2hd6kg2vvdsf1jm63l3ofh4gc45ai.apps.googleusercontent.com"
    }
}
