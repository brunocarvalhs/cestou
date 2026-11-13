package br.com.brunocarvalhs.howmuch.core.ads.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.brunocarvalhs.howmuch.core.ads.service.AdsEntryPoint
import br.com.brunocarvalhs.howmuch.core.ui.model.AdPlacement
import br.com.brunocarvalhs.howmuch.core.ui.util.findActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import dagger.hilt.android.EntryPointAccessors

/**
 * Banner de anúncio para usuários FREE. Ocupa espaço só quando o [br.com.brunocarvalhs.howmuch.core.ads.service.AdsPolicy]
 * permite; para PRO ou com a flag desligada, não compõe nada, então não há espaço vazio.
 *
 * Fornecido ao restante do app pelo `:app` via `LocalAdBanner`. Não use em telas de
 * autenticação, onboarding ou paywall (sdd-059).
 */
@Composable
fun AdBanner(placement: AdPlacement, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entry = remember(context) {
        EntryPointAccessors.fromApplication(context.applicationContext, AdsEntryPoint::class.java)
    }
    val canShow by remember(entry) { entry.adsPolicy().observeBannerAllowed() }
        .collectAsStateWithLifecycle(initialValue = false)

    if (canShow) {
        BannerSlot(placement = placement, entry = entry, modifier = modifier)
    }
}

@Composable
private fun BannerSlot(
    placement: AdPlacement,
    entry: AdsEntryPoint,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val bannerUnitId = remember(entry) { entry.adUnitIds().banner() }

    // Um AdView por contexto: o SDK não deve receber um novo a cada recomposição.
    val adView = remember(context, bannerUnitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = bannerUnitId
        }
    }

    LaunchedEffect(activity) {
        if (activity != null && entry.adsSdk().initializeWithConsent(activity)) {
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    AndroidView(
        factory = { adView },
        modifier = modifier
            .fillMaxWidth()
            .testTag("ad_banner_${placement.name.lowercase()}"),
    )
}
