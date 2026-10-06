package br.com.brunocarvalhs.howmuch.feature.auth.presentation.state

import androidx.compose.runtime.Immutable
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameResult
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameValidator

@Immutable
internal data class CompleteNameUiState(
    val givenName: String = "",
    val familyName: String = "",
    val givenNameError: PersonNameError? = null,
    val familyNameError: PersonNameError? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val hasSaveError: Boolean = false,
) {
    val isSubmitEnabled: Boolean
        get() = !isLoading && PersonNameValidator.validate(givenName, familyName) is PersonNameResult.Valid
}
