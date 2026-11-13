package br.com.brunocarvalhs.howmuch.core.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Activity que hospeda este contexto, se houver. Anúncios em tela cheia e consentimento precisam dela. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
