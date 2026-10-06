package br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthFormScaffold
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthSubmitButton
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.AuthTextLink
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email.PersonNameFields
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.CompleteNameIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.CompleteNameUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.NameField

/**
 * Spec EPA-06: shown to any signed-in account without a name. There is no back/skip; the only
 * other way out is signing in with another account.
 */
@Composable
internal fun CompleteNameScreen(
    state: CompleteNameUiState,
    intent: CompleteNameIntent,
    onExitAttempt: () -> Unit = {},
) {
    BackHandler(onBack = onExitAttempt)

    AuthFormScaffold(
        title = stringResource(R.string.name_step_title),
        subtitle = stringResource(R.string.name_step_description),
        onBack = null
    ) {
        if (state.hasSaveError) {
            Text(
                text = stringResource(R.string.name_step_save_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name_step_save_error")
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }

        PersonNameFields(
            givenName = state.givenName,
            familyName = state.familyName,
            givenNameError = state.givenNameError,
            familyNameError = state.familyNameError,
            enabled = !state.isLoading,
            testTagPrefix = "name_step",
            onGivenNameChange = intent.onGivenNameChange,
            onFamilyNameChange = intent.onFamilyNameChange,
            onGivenNameFocusLost = { intent.onFieldFocusLost(NameField.GIVEN_NAME) },
            onFamilyNameFocusLost = { intent.onFieldFocusLost(NameField.FAMILY_NAME) },
            familyNameImeAction = ImeAction.Done,
            onFamilyNameDone = { if (state.isSubmitEnabled) intent.onSubmit() },
        )

        AuthSubmitButton(
            text = stringResource(R.string.name_step_continue),
            enabled = state.isSubmitEnabled,
            isLoading = state.isLoading,
            testTag = "name_step_submit_button",
            onClick = intent.onSubmit
        )

        AuthTextLink(
            text = stringResource(R.string.name_step_use_another_account),
            testTag = "name_step_use_another_account",
            onClick = intent.onUseAnotherAccount
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompleteNameScreenPreview() {
    CestouTheme {
        CompleteNameScreen(
            state = CompleteNameUiState(givenName = "Ana", familyNameError = PersonNameError.EMPTY),
            intent = CompleteNameIntent()
        )
    }
}
