package br.com.brunocarvalhs.howmuch.core.ads.service

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inicializa o SDK do AdMob uma única vez por processo. A inicialização só acontece depois que
 * o consentimento permitir pedir anúncios ([ConsentGateway]).
 */
@Singleton
internal class AdsSdk @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consent: ConsentGateway,
) {

    private val initialized = AtomicBoolean(false)

    /** Consentimento e inicialização. Retorna `true` quando anúncios podem ser carregados. */
    suspend fun initializeWithConsent(activity: Activity): Boolean {
        if (!consent.ensureConsent(activity)) return false
        if (initialized.compareAndSet(false, true)) {
            MobileAds.initialize(context) { }
        }
        return true
    }
}
