package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * FirebaseUI shows this after a Google sign-in, only until `onSignInSuccess` navigates away.
 * E-mail sign-in never reaches FirebaseUI, so there is no verification/MFA screen to render.
 */
@Composable
internal fun CustomAuthenticatedContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
