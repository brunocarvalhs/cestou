package br.com.brunocarvalhs.howmuch.feature.auth.presentation.firebaseui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import javax.inject.Inject

/** Mints a fresh Google ID token on the phone, for the watch to sign in to Firebase with (AD-009). */
internal class GoogleIdTokenProvider @Inject constructor() {

    /**
     * Returns a credential for a Google account that already authorized this app, or null when there
     * is none (e.g. the user signs in with e-mail) or the user dismissed the picker.
     * [activityContext] must be an Activity: Credential Manager may show UI.
     */
    suspend fun get(activityContext: Context): GoogleIdTokenCredential? = try {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(GoogleAuthProviderFactory.SERVER_CLIENT_ID)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = CredentialManager.create(activityContext).getCredential(activityContext, request)
        GoogleIdTokenCredential.createFrom(response.credential.data)
    } catch (_: GetCredentialException) {
        null
    } catch (_: GoogleIdTokenParsingException) {
        null
    }
}
