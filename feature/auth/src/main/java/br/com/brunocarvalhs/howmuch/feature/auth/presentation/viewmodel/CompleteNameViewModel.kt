package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameResult
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameValidator
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.CompleteNameIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.CompleteNameUiState
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.NameField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Required-name step for signed-in accounts without a name (spec EPA-06). Cannot be skipped. */
@HiltViewModel
internal class CompleteNameViewModel @Inject constructor(
    private val authService: AuthService,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState = _uiState.asStateFlow()

    val intent = CompleteNameIntent(
        onGivenNameChange = { value -> _uiState.update { it.copy(givenName = value, givenNameError = null) } },
        onFamilyNameChange = { value -> _uiState.update { it.copy(familyName = value, familyNameError = null) } },
        onFieldFocusLost = ::validateField,
        onSubmit = ::submit,
        onUseAnotherAccount = { viewModelScope.launch { authService.signOut() } },
    )

    /** An existing one-word name goes into the first-name field (spec EPA-13 item 13). */
    private fun initialState(): CompleteNameUiState {
        val words = authService.currentUser?.displayName?.trim()?.split(Regex("\\s+"))
            ?.filter { it.isNotEmpty() }.orEmpty()
        return CompleteNameUiState(
            givenName = words.firstOrNull().orEmpty(),
            familyName = words.drop(1).joinToString(" "),
        )
    }

    private fun validateField(field: NameField) {
        val (givenError, familyError) = nameErrors(_uiState.value.givenName, _uiState.value.familyName)
        val error = if (field == NameField.GIVEN_NAME) givenError else familyError
        analyticsTracker.trackNameError(error, SOURCE)
        _uiState.update {
            if (field == NameField.GIVEN_NAME) it.copy(givenNameError = error) else it.copy(familyNameError = error)
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        val name = PersonNameValidator.validate(state.givenName, state.familyName)
        if (name !is PersonNameResult.Valid) {
            val (givenError, familyError) = nameErrors(state.givenName, state.familyName)
            _uiState.update { it.copy(givenNameError = givenError, familyNameError = familyError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, hasSaveError = false) }
        viewModelScope.launch {
            authService.updateDisplayName(name.fullName)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSuccess = true) } }
                .onFailure { _uiState.update { it.copy(isLoading = false, hasSaveError = true) } }
        }
    }

    private companion object {
        const val SOURCE = "name_step"
    }
}
