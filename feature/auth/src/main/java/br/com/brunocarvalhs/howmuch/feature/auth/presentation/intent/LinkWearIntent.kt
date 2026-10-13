package br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent

import android.content.Context

internal data class LinkWearIntent(
    val onCodeChange: (String) -> Unit = {},
    /** Takes the Activity context Credential Manager needs; not kept by the ViewModel. */
    val onLinkClick: (Context) -> Unit = {}
)
