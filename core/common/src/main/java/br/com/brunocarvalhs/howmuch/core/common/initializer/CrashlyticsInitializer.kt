package br.com.brunocarvalhs.howmuch.core.common.initializer

import android.content.Context
import android.os.Build
import androidx.startup.Initializer
import br.com.brunocarvalhs.howmuch.core.common.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashlyticsInitializer : Initializer<FirebaseCrashlytics> {

    override fun create(context: Context): FirebaseCrashlytics {
        return FirebaseCrashlytics.getInstance().apply {
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            setStaticKeys()
        }
    }

    private fun FirebaseCrashlytics.setStaticKeys() {
        setCustomKey(KEY_BUILD_TYPE, BuildConfig.BUILD_TYPE)
        setCustomKey(KEY_VERSION_NAME, BuildConfig.VERSION_NAME)
        setCustomKey(KEY_DEVICE_MANUFACTURER, Build.MANUFACTURER)
        setCustomKey(KEY_DEVICE_MODEL, Build.MODEL)
        setCustomKey(KEY_OS_VERSION, Build.VERSION.SDK_INT)
    }

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
