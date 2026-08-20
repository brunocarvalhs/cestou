package br.com.brunocarvalhs.howmuch.core.common.initializer

import android.content.Context
import androidx.startup.Initializer
import br.com.brunocarvalhs.howmuch.core.common.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import timber.log.Timber

class FirebaseInitializer : Initializer<FirebaseApp> {
    override fun create(context: Context): FirebaseApp {
        val app = FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()

        if (BuildConfig.DEBUG) {
            Firebase.appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )

            Firebase.appCheck
                .getAppCheckToken(false)
                .addOnSuccessListener {
                    Timber.d("Firebase AppCheck Token: ${it.token}")
                }
                .addOnFailureListener {
                    Timber.e(it, "Firebase AppCheck Erro")
                }
        }

        return app
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
