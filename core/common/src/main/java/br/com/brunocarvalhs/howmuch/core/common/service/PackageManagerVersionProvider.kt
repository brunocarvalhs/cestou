package br.com.brunocarvalhs.howmuch.core.common.service

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.common.contract.AppVersionProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Lê o versionName real instalado via PackageManager, já sem o sufixo de build type
 * (ex: "1.3.0-debug" vira "1.3.0"), para que a comparação de versão seja consistente
 * entre debug e release.
 */
class PackageManagerVersionProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AppVersionProvider {

    override fun versionName(): String {
        val versionName = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
        return versionName.orEmpty().substringBefore("-")
    }
}
