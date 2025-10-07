package br.com.brunocarvalhs.howmuch.app.foundation.extensions

import android.view.Window
import androidx.core.view.WindowCompat

fun Window.setStatusBarIconColor(isDark: Boolean) {
    val insetsController = WindowCompat.getInsetsController(this, this.decorView)
    insetsController.isAppearanceLightStatusBars = !isDark
}
