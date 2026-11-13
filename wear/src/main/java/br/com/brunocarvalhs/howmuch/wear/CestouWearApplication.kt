package br.com.brunocarvalhs.howmuch.wear

import android.app.Application
import androidx.wear.tiles.TileService
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsUserProperties
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListStore
import br.com.brunocarvalhs.howmuch.wear.presentation.tile.LastListTileService
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CestouWearApplication : Application() {

    @Inject
    lateinit var wearListStore: WearListStore

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // O wear compartilha o applicationId do celular: sem essa marca, GA4 e Crashlytics
        // misturam os dados dos dois dispositivos.
        analyticsTracker.setUserProperty(
            AnalyticsUserProperties.FORM_FACTOR,
            AnalyticsUserProperties.FORM_FACTOR_WEAR
        )
        FirebaseCrashlytics.getInstance().setCustomKey(
            AnalyticsUserProperties.FORM_FACTOR,
            AnalyticsUserProperties.FORM_FACTOR_WEAR
        )
        refreshTileOnListChange()
    }

    // The tile can't keep a listener open, so push it a refresh whenever the saved summary changes.
    private fun refreshTileOnListChange() {
        scope.launch {
            wearListStore.lastList
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    TileService.getUpdater(this@CestouWearApplication)
                        .requestUpdate(LastListTileService::class.java)
                }
        }
    }
}
