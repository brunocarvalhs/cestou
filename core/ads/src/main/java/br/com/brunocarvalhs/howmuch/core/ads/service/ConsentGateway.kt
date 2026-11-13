package br.com.brunocarvalhs.howmuch.core.ads.service

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Consentimento de privacidade via Google User Messaging Platform (LGPD/GDPR). Atualiza o
 * status e mostra o formulário só quando a região exigir. Qualquer falha cai no estado atual
 * do consentimento, sem travar a tela.
 */
@Singleton
internal class ConsentGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Retorna `true` quando anúncios podem ser pedidos. */
    suspend fun ensureConsent(activity: Activity): Boolean =
        suspendCancellableCoroutine { continuation ->
            val consentInformation = UserMessagingPlatform.getConsentInformation(context)

            fun finish() {
                if (continuation.isActive) continuation.resume(consentInformation.canRequestAds())
            }

            consentInformation.requestConsentInfoUpdate(
                activity,
                ConsentRequestParameters.Builder().build(),
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { finish() }
                },
                { finish() },
            )
        }
}
