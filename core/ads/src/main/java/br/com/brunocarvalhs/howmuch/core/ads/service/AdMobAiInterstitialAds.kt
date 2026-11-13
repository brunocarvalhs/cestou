package br.com.brunocarvalhs.howmuch.core.ads.service

import android.app.Activity
import android.content.Context
import br.com.brunocarvalhs.howmuch.core.ui.contract.AiInterstitialHook
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intersticial sobre o AdMob. Mantém um único anúncio carregado: ao mostrar, já começa a
 * carregar o próximo. Toda a interação acontece na thread principal (callbacks do SDK e
 * composição), por isso o estado fica sem sincronização.
 */
@Singleton
internal class AdMobAiInterstitialAds @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sdk: AdsSdk,
    private val adUnitIds: AdUnitIds,
) : AiInterstitialHook {

    private var loaded: InterstitialAd? = null
    private var loading = false

    override suspend fun prepare(activity: Activity) {
        if (sdk.initializeWithConsent(activity)) load()
    }

    override fun showIfReady(activity: Activity): Boolean {
        val ad = loaded
        if (ad == null) {
            load()
            return false
        }
        loaded = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = load()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = load()
        }
        ad.show(activity)
        return true
    }

    private fun load() {
        if (loaded != null || loading) return
        loading = true
        InterstitialAd.load(
            context,
            adUnitIds.interstitial(),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loaded = ad
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    Timber.w("Intersticial de IA não carregou: code=%d", error.code)
                }
            },
        )
    }
}
