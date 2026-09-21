package br.com.brunocarvalhs.howmuch.core.remoteconfig.initializer

import android.content.Context
import androidx.startup.Initializer
import br.com.brunocarvalhs.howmuch.core.common.initializer.FirebaseInitializer
import br.com.brunocarvalhs.howmuch.core.remoteconfig.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
import timber.log.Timber

/**
 * Dispara o primeiro fetch/activate do Remote Config assim que o app inicia, para que
 * flags e variáveis remotas estejam disponíveis o quanto antes. Enquanto o fetch não
 * termina (ou falha), quem consulta [br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService]
 * recebe o default passado por ele mesmo.
 */
class RemoteConfigInitializer : Initializer<FirebaseRemoteConfig> {

    override fun create(context: Context): FirebaseRemoteConfig {
        val remoteConfig = Firebase.remoteConfig
        val minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0L else MINIMUM_FETCH_INTERVAL_SECONDS

        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(minimumFetchIntervalInSeconds)
                .build()
        )

        remoteConfig.fetchAndActivate()
            .addOnSuccessListener { activated -> Timber.d("RemoteConfig fetch concluído (activated=$activated)") }
            .addOnFailureListener { error -> Timber.e(error, "RemoteConfig fetch falhou, usando defaults locais") }

        return remoteConfig
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        FirebaseInitializer::class.java
    )

    companion object {
        private const val MINIMUM_FETCH_INTERVAL_SECONDS = 3600L
    }
}
