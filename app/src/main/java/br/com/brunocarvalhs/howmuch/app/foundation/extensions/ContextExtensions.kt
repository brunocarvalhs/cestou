package br.com.brunocarvalhs.howmuch.app.foundation.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun Context.isFirstAppOpen(): Boolean = withContext(Dispatchers.IO) {
    val prefs = getSharedPreferences(packageName, Context.MODE_PRIVATE)
    val isFirst = prefs.getBoolean("FIRST_APP_OPEN", true)
    if (isFirst) {
        prefs.edit { putBoolean("FIRST_APP_OPEN", false) }
    }
    isFirst
}

@SuppressLint("HardwareIds")
fun Context.getId(): String {
    return Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
}

internal fun Context.shareText(subject: String, text: String) {
    val intent = Intent().shareText(subject = subject, text = text)
    val chooser = Intent.createChooser(intent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    this.startActivity(chooser)
}

internal fun Context.openUrl(url: String) {
    val intent = Intent().openUrl(url = url).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    this.startActivity(intent)
}

internal fun Context.openPlayStore() {
    val intent = Intent().openPlayStore(packageName = this.packageName).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    this.startActivity(intent)
}

internal fun Context.getAppVersion(): String {
    return try {
        val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
        packageInfo.versionName ?: "1.0"
    } catch (_: Exception) {
        "1.0"
    }
}
