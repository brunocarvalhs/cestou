package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameResult
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameValidator

internal fun nameErrors(givenName: String, familyName: String): Pair<PersonNameError?, PersonNameError?> =
    when (val result = PersonNameValidator.validate(givenName, familyName)) {
        is PersonNameResult.Valid -> null to null
        is PersonNameResult.Invalid -> result.givenNameError to result.familyNameError
    }

/** Tracks only the rule category, never the typed name (spec EPA-13 item 16). Empty fields are not tracked. */
internal fun AnalyticsTracker.trackNameError(error: PersonNameError?, source: String) {
    val category = when (error) {
        null, PersonNameError.EMPTY -> return
        PersonNameError.TOO_SHORT, PersonNameError.TOO_LONG, PersonNameError.FULL_NAME_TOO_LONG -> "length"
        PersonNameError.INVALID_CHARACTERS -> "characters"
        PersonNameError.CONTACT_INFO -> "contact"
        PersonNameError.NOT_A_NAME -> "blocked"
    }
    trackEvent(
        AnalyticsEvents.AUTH_NAME_VALIDATION_FAILED,
        mapOf(AnalyticsParams.REASON to category, AnalyticsParams.SOURCE to source)
    )
}
