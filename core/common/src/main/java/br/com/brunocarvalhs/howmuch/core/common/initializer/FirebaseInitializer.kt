package br.com.brunocarvalhs.howmuch.core.common.initializer

import android.content.Context
import androidx.startup.Initializer
import br.com.brunocarvalhs.howmuch.core.common.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import timber.log.Timber

class FirebaseInitializer : Initializer<FirebaseApp> {
    override fun create(context: Context): FirebaseApp {
        val existingApp = FirebaseApp.getApps(context).firstOrNull()
        if (existingApp != null) return existingApp

        val options = FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
            .setGcmSenderId(BuildConfig.FIREBASE_PROJECT_NUMBER)
            .build()

        val app = FirebaseApp.initializeApp(context, options)

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
