package br.com.brunocarvalhs.howmuch.feature.auth.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomAuthenticatedContent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomEmailContent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomMethodPickerLayout
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomMethodPickerTerms
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomMfaChallengeContent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomMfaEnrollmentContent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomPhoneContent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomReauthContent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen.WelcomeScreen
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel.WelcomeViewModel
import com.firebase.ui.auth.ui.method_picker.MethodPickerTermsConfiguration
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen

internal fun NavGraphBuilder.authGraph(
    navigator: Navigator,
    onAuthSuccess: () -> Unit
) {
    composable<Welcome> {
        val viewModel: WelcomeViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        
        FirebaseAuthScreen(
            configuration = viewModel.authConfig.invoke(),
            onSignInSuccess = { _ ->
                onAuthSuccess()
            },
            onSignInFailure = { exception ->
                viewModel.intent.onSignInFailure(exception)
            },
            onSignInCancelled = {

            },
            customMethodPickerLayout = { providers, onProviderSelected ->
                WelcomeScreen(
                    state = uiState,
                    intent = viewModel.intent
                ) {
                    CustomMethodPickerLayout(providers, onProviderSelected)
                }
            },
            customMethodPickerTermsConfiguration = MethodPickerTermsConfiguration(
                content = { CustomMethodPickerTerms() }
            ),
            emailContent = { CustomEmailContent(it) },
            phoneContent = { CustomPhoneContent(it) },
            mfaEnrollmentContent = { CustomMfaEnrollmentContent(it) },
            mfaChallengeContent = { CustomMfaChallengeContent(it) },
            reauthContent = { rauthRequired, onCancel ->
                CustomReauthContent(rauthRequired, onCancel)
            },
            authenticatedContent = { authState, uiContext ->
                CustomAuthenticatedContent(authState, uiContext)
            }
        )
    }
}
