package br.com.brunocarvalhs.howmuch.app.foundation.extensions

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri

@SuppressLint("UseKtx")
internal fun Intent.openUrl(url: String) = apply {
    this.action = Intent.ACTION_VIEW
    this.data = Uri.parse(url)
}

@SuppressLint("UseKtx")
internal fun Intent.openPlayStore(packageName: String) = apply {
    this.action = Intent.ACTION_VIEW
    this.data = Uri.parse("market://details?id=$packageName")
}

internal fun Intent.shareText(subject: String, text: String) = apply {
    this.action = Intent.ACTION_SEND
    this.type = "text/plain"
    this.putExtra(Intent.EXTRA_SUBJECT, subject)
    this.putExtra(Intent.EXTRA_TEXT, text)
}
