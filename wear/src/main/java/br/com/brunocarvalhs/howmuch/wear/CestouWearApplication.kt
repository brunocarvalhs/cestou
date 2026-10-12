package br.com.brunocarvalhs.howmuch.wear

import android.app.Application
import androidx.wear.tiles.TileService
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListStore
import br.com.brunocarvalhs.howmuch.wear.presentation.tile.LastListTileService
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        if (br.com.brunocarvalhs.howmuch.wear.BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
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
