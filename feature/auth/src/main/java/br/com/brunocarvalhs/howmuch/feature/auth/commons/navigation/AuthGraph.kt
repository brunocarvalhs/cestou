package br.com.brunocarvalhs.howmuch.feature.auth.commons.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.components.auth.CustomAuthenticatedContent
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.components.auth.CustomEmailContent
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.components.auth.CustomMethodPickerLayout
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.components.auth.CustomMethodPickerTerms
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.components.auth.CustomMfaChallengeContent
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.components.auth.CustomMfaEnrollmentContent
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.components.auth.CustomPhoneContent
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.components.auth.CustomReauthContent
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.screen.WelcomeScreen
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.viewmodel.LoginViewModel
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.viewmodel.WelcomeViewModel
import com.firebase.ui.auth.ui.method_picker.MethodPickerTermsConfiguration
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

fun NavGraphBuilder.authGraph(
    navigator: Navigator,
    onAuthSuccess: () -> Unit
) {
    composable<Welcome> {
        val viewModel: WelcomeViewModel = hiltViewModel()
        viewModel.onNavigateToLogin = {
            navigator.navigate(Login)
        }
        
        val state by viewModel.uiState.collectAsState()
        WelcomeScreen(
            state = state,
            intent = viewModel.intent
        )
    }

    composable<Login> {
        val viewModel: LoginViewModel = hiltViewModel()
        
        FirebaseAuthScreen(
            configuration = viewModel.authConfig.invoke(),
            onSignInSuccess = { _ ->
                onAuthSuccess()
            },
            onSignInFailure = { exception ->
                viewModel.onSignInFailure(exception)
            },
            onSignInCancelled = {
                navigator.goBack()
            },
            customMethodPickerLayout = { providers, onProviderSelected ->
                CustomMethodPickerLayout(providers, onProviderSelected)
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
