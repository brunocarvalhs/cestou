package br.com.brunocarvalhs.howmuch.feature.auth.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomAuthenticatedContent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomMethodPickerLayout
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomMethodPickerTerms
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomMfaChallengeContent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomMfaEnrollmentContent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.auth.CustomReauthContent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen.CompleteNameScreen
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen.EmailSignInScreen
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen.EmailSignUpScreen
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen.PasswordResetScreen
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen.WelcomeScreen
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel.CompleteNameViewModel
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel.EmailSignInViewModel
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel.EmailSignUpViewModel
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel.PasswordResetViewModel
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel.WelcomeViewModel
import com.firebase.ui.auth.ui.method_picker.MethodPickerTermsConfiguration
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen

internal fun NavGraphBuilder.authGraph(
    navigator: Navigator,
    onAuthSuccess: () -> Unit,
    onNameCompleted: () -> Unit
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
                    CustomMethodPickerLayout(
                        providers = providers,
                        onProviderSelected = onProviderSelected,
                        onEmailSelected = { navigator.navigate(EmailSignIn()) }
                    )
                }
            },
            customMethodPickerTermsConfiguration = MethodPickerTermsConfiguration(
                content = { CustomMethodPickerTerms() }
            ),
            // FirebaseUI hosts only Google (see AuthConfigUseCase). E-mail is our own flow below,
            // on FirebaseAuth, so no emailContent/phoneContent slot is wired up here.
            mfaEnrollmentContent = { CustomMfaEnrollmentContent(it) },
            mfaChallengeContent = { CustomMfaChallengeContent(it) },
            reauthContent = { state ->
                CustomReauthContent(state)
            },
            authenticatedContent = { _, _ -> CustomAuthenticatedContent() }
        )
    }

    emailSignInDestination(navigator, onAuthSuccess)
    emailSignUpDestination(navigator, onAuthSuccess)
    passwordResetDestination(navigator)
    completeNameDestination(onNameCompleted)
}

private fun NavGraphBuilder.emailSignInDestination(navigator: Navigator, onAuthSuccess: () -> Unit) {
    composable<EmailSignIn> { entry ->
        val viewModel: EmailSignInViewModel = hiltViewModel()
        val email = entry.toRoute<EmailSignIn>().email
        LaunchedEffect(viewModel, email) { viewModel.start(email) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(state.isSuccess) { if (state.isSuccess) onAuthSuccess() }

        EmailSignInScreen(
            state = state,
            intent = viewModel.intent,
            onBack = { navigator.goBack() },
            onForgotPassword = { navigator.navigate(PasswordReset(email = it)) },
            onCreateAccount = { navigator.navigate(EmailSignUp) }
        )
    }
}

private fun NavGraphBuilder.emailSignUpDestination(navigator: Navigator, onAuthSuccess: () -> Unit) {
    composable<EmailSignUp> {
        val viewModel: EmailSignUpViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(state.isSuccess) { if (state.isSuccess) onAuthSuccess() }

        EmailSignUpScreen(
            state = state,
            intent = viewModel.intent,
            onBack = { navigator.goBack() },
            onGoToSignIn = { email ->
                navigator.navigate(EmailSignIn(email = email)) {
                    popUpTo(Welcome)
                }
            }
        )
    }
}

private fun NavGraphBuilder.passwordResetDestination(navigator: Navigator) {
    composable<PasswordReset> { entry ->
        val viewModel: PasswordResetViewModel = hiltViewModel()
        val email = entry.toRoute<PasswordReset>().email
        LaunchedEffect(viewModel, email) { viewModel.start(email) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        PasswordResetScreen(state = state, intent = viewModel.intent, onBack = { navigator.goBack() })
    }
}

private fun NavGraphBuilder.completeNameDestination(onNameCompleted: () -> Unit) {
    composable<CompleteName> {
        val viewModel: CompleteNameViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val activity = LocalActivity.current
        LaunchedEffect(state.isSuccess) { if (state.isSuccess) onNameCompleted() }

        // Back leaves the app instead of reaching any screen without a name (spec EPA-06).
        CompleteNameScreen(state = state, intent = viewModel.intent, onExitAttempt = { activity?.finish() })
    }
}
