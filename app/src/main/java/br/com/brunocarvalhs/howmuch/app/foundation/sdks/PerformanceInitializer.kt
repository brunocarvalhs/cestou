package br.com.brunocarvalhs.howmuch.app.foundation.sdks

import android.content.Context
import androidx.startup.Initializer
import br.com.brunocarvalhs.howmuch.BuildConfig
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.getId
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.AddTrace

class PerformanceInitializer : Initializer<Unit> {

    @AddTrace(name = "SDK-Firebase-Performance")
    override fun create(context: Context) {
        FirebasePerformance.getInstance().apply {
            isPerformanceCollectionEnabled = BuildConfig.DEBUG.not()
            putAttribute(DEVICE_ID, context.getId())
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(FirebaseInitializer::class.java)
    }

    companion object {
        const val DEVICE_ID = "deviceId"
    }
}
