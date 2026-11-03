package br.com.brunocarvalhs.howmuch.analytics

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PLAY_STORE = "com.android.vending"

/**
 * Diz se esta instalação veio da Play Store. Qualquer outra origem (Firebase App Distribution,
 * adb, APK direto) é tratada como tester e separada das métricas de clientes reais.
 */
@Singleton
internal class InstallSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isTester: Boolean by lazy { installer() != PLAY_STORE }

    private fun installer(): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            context.packageManager.getInstallerPackageName(context.packageName)
        }
    }.getOrNull()
}
