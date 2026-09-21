package br.com.brunocarvalhs.howmuch.core.common.initializer

import android.content.Context
import androidx.startup.Initializer
import br.com.brunocarvalhs.howmuch.core.common.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import timber.log.Timber

class FirebaseInitializer : Initializer<FirebaseApp> {
    override fun create(context: Context): FirebaseApp {
        val app = FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()

        // A release build with no provider installed at all still calls backend services that
        // enforce App Check - those calls are silently rejected, which can hang the app on
        // startup if something on the launch path blocks on one (e.g. an initial auth/Firestore
        // read). Debug builds use the Debug provider (token registered manually in the Firebase
        // console); everything else must use Play Integrity, the real attestation provider.
        val providerFactory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        Firebase.appCheck.installAppCheckProviderFactory(providerFactory)

        if (BuildConfig.DEBUG) {
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
