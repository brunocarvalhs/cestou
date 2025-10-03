package br.com.brunocarvalhs.howmuch.app.foundation.sdks

import android.content.Context
import androidx.startup.Initializer
import com.google.firebase.FirebaseApp
import com.google.firebase.perf.metrics.AddTrace

class FirebaseInitializer : Initializer<Unit> {

    @AddTrace(name = "SDK-Firebase")
    override fun create(context: Context) {
        FirebaseApp.initializeApp(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
