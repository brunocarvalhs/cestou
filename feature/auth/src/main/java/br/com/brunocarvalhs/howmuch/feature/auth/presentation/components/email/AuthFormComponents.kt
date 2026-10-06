package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components.email

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTextField
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouTopBar
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailFieldError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.PasswordFieldError

/** Scrollable, keyboard-aware frame shared by the e-mail and name screens. */
@Composable
internal fun AuthFormScaffold(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CestouTopBar(
                title = {},
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.auth_navigate_back)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

/** Primary action; disabled while loading so it can't be tapped twice, with progress shown under it. */
@Composable
internal fun AuthSubmitButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CestouButton(
            text = text,
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = Modifier.testTag(testTag)
        )
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Form-level failure (network, wrong credentials...), announced by TalkBack. */
@Composable
internal fun AuthFormError(error: EmailAuthError?, testTag: String) {
    if (error == null) return
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(error.messageRes()),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
internal fun AuthTextLink(text: String, testTag: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Required first + last name pair (spec EPA-13 item 12): word capitalization, no autocorrect,
 * autofill hints, Next between fields, and errors that appear only after leaving a field.
 */
@Composable
internal fun PersonNameFields(
    givenName: String,
    familyName: String,
    givenNameError: PersonNameError?,
    familyNameError: PersonNameError?,
    enabled: Boolean,
    testTagPrefix: String,
    onGivenNameChange: (String) -> Unit,
    onFamilyNameChange: (String) -> Unit,
    onGivenNameFocusLost: () -> Unit,
    onFamilyNameFocusLost: () -> Unit,
    familyNameImeAction: ImeAction = ImeAction.Next,
    onFamilyNameDone: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val nameKeyboard = KeyboardOptions(
        capitalization = KeyboardCapitalization.Words,
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next,
    )
    CestouTextField(
        value = givenName,
        onValueChange = onGivenNameChange,
        label = stringResource(R.string.auth_field_given_name),
        keyboardOptions = nameKeyboard,
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        isError = givenNameError != null,
        errorMessage = givenNameError?.let { stringResource(it.messageRes(isFamilyName = false)) },
        enabled = enabled,
        testTag = "${testTagPrefix}_given_name_field",
        textFieldModifier = Modifier
            .onFocusLost(onGivenNameFocusLost)
            .semantics { contentType = ContentType.PersonFirstName }
    )
    CestouTextField(
        value = familyName,
        onValueChange = onFamilyNameChange,
        label = stringResource(R.string.auth_field_family_name),
        keyboardOptions = nameKeyboard.copy(imeAction = familyNameImeAction),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = {
                focusManager.clearFocus()
                onFamilyNameDone()
            }
        ),
        isError = familyNameError != null,
        errorMessage = familyNameError?.let { stringResource(it.messageRes(isFamilyName = true)) },
        supportingText = stringResource(R.string.auth_family_name_help),
        enabled = enabled,
        testTag = "${testTagPrefix}_family_name_field",
        textFieldModifier = Modifier
            .onFocusLost(onFamilyNameFocusLost)
            .semantics { contentType = ContentType.PersonLastName }
    )
}

/** Fires once each time the field goes from focused to unfocused. */
@Composable
internal fun Modifier.onFocusLost(block: () -> Unit): Modifier {
    var hadFocus by remember { mutableStateOf(false) }
    return onFocusChanged { state ->
        if (hadFocus && !state.isFocused) block()
        hadFocus = state.isFocused
    }
}

@StringRes
internal fun PersonNameError.messageRes(isFamilyName: Boolean): Int = when (this) {
    PersonNameError.EMPTY -> if (isFamilyName) R.string.name_error_family_empty else R.string.name_error_given_empty
    PersonNameError.TOO_SHORT -> R.string.name_error_too_short
    PersonNameError.TOO_LONG -> R.string.name_error_too_long
    PersonNameError.FULL_NAME_TOO_LONG -> R.string.name_error_full_too_long
    PersonNameError.INVALID_CHARACTERS -> R.string.name_error_invalid_characters
    PersonNameError.CONTACT_INFO ->
        if (isFamilyName) R.string.name_error_family_contact else R.string.name_error_given_contact
    PersonNameError.NOT_A_NAME ->
        if (isFamilyName) R.string.name_error_family_blocked else R.string.name_error_given_blocked
}

@StringRes
internal fun EmailFieldError.messageRes(): Int = when (this) {
    EmailFieldError.EMPTY -> R.string.email_error_empty
    EmailFieldError.INVALID -> R.string.email_error_invalid
    EmailFieldError.ALREADY_IN_USE -> R.string.email_error_in_use
}

@StringRes
internal fun PasswordFieldError.messageRes(): Int = when (this) {
    PasswordFieldError.EMPTY -> R.string.password_error_empty
    PasswordFieldError.TOO_SHORT -> R.string.password_error_too_short
}

@StringRes
internal fun EmailAuthError.messageRes(): Int = when (this) {
    EmailAuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    EmailAuthError.TOO_MANY_REQUESTS -> R.string.auth_error_too_many_requests
    EmailAuthError.USER_DISABLED -> R.string.auth_error_user_disabled
    EmailAuthError.NETWORK -> R.string.auth_error_network
    EmailAuthError.INVALID_EMAIL -> R.string.email_error_invalid
    EmailAuthError.EMAIL_ALREADY_IN_USE -> R.string.email_error_in_use
    EmailAuthError.WEAK_PASSWORD -> R.string.password_error_too_short
    EmailAuthError.UNKNOWN -> R.string.auth_error_unknown
}
