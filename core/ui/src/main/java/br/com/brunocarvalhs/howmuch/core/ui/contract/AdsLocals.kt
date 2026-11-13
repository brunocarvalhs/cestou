package br.com.brunocarvalhs.howmuch.core.ui.contract

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import br.com.brunocarvalhs.howmuch.core.ui.model.AdPlacement

/**
 * Os dois locals abaixo são a costura entre as features e a implementação de anúncios. São
 * exceção à regra de evitar novos `CompositionLocal` (registrada no baseline do detekt, como
 * `LocalCurrency`): os módulos de feature também são compilados no `:wear`, que não pode
 * depender do SDK de anúncios. O `:app` fornece as implementações; sem provedor, o padrão não
 * faz nada.
 */

/** Banner de anúncio, fornecido pelo `:app`. O padrão não desenha nada. */
val LocalAdBanner: ProvidableCompositionLocal<@Composable (AdPlacement, Modifier) -> Unit> =
    compositionLocalOf { { _, _ -> } }

/** Intersticial fornecido pelo `:app`. O padrão nunca mostra anúncio. */
val LocalAiInterstitial: ProvidableCompositionLocal<AiInterstitialHook> = compositionLocalOf {
    object : AiInterstitialHook {
        override suspend fun prepare(activity: Activity) = Unit
        override fun showIfReady(activity: Activity): Boolean = false
    }
}
