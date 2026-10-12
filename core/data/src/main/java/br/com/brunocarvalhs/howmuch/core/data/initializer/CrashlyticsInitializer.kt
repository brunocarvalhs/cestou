package br.com.brunocarvalhs.howmuch.core.data.initializer

import android.content.Context
import android.os.Build
import androidx.startup.Initializer
import br.com.brunocarvalhs.howmuch.core.data.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashlyticsInitializer : Initializer<FirebaseCrashlytics> {

    override fun create(context: Context): FirebaseCrashlytics {
        return FirebaseCrashlytics.getInstance().apply {
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            setStaticKeys(context.appVersionName())
        }
    }

    private fun FirebaseCrashlytics.setStaticKeys(versionName: String) {
        setCustomKey(KEY_BUILD_TYPE, BuildConfig.BUILD_TYPE)
        setCustomKey(KEY_VERSION_NAME, versionName)
        setCustomKey(KEY_DEVICE_MANUFACTURER, Build.MANUFACTURER)
        setCustomKey(KEY_DEVICE_MODEL, Build.MODEL)
        setCustomKey(KEY_OS_VERSION, Build.VERSION.SDK_INT)
    }

    // The app's real version (core:common's BuildConfig.VERSION_NAME was a hardcoded "1.0").
    private fun Context.appVersionName(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        FirebaseInitializer::class.java
    )

    companion object {
        const val KEY_BUILD_TYPE = "build_type"
        const val KEY_VERSION_NAME = "app_version_name"
        const val KEY_DEVICE_MANUFACTURER = "device_manufacturer"
        const val KEY_DEVICE_MODEL = "device_model"
        const val KEY_OS_VERSION = "os_version"
    }
}
