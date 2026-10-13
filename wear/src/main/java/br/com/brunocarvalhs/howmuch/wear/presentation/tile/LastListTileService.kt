package br.com.brunocarvalhs.howmuch.wear.presentation.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListStore
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListSummary
import br.com.brunocarvalhs.howmuch.wear.presentation.MainActivity
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import java.text.NumberFormat
import javax.inject.Inject
import kotlin.math.abs

private const val RESOURCES_VERSION = "1"
private const val COLOR_TEXT = 0xFFFFFFFF.toInt()
private const val COLOR_MUTED = 0xFFBDBDBD.toInt()
private const val COLOR_OK = 0xFF81C784.toInt()
private const val COLOR_OVER = 0xFFE57373.toInt()
private const val SIZE_LABEL = 14f
private const val SIZE_TOTAL = 24f
private const val SIZE_DETAIL = 12f

/**
 * Glanceable summary of the last list opened on the watch: title, total, budget and items left.
 * Reads the summary the list screen saves on every change (no Firestore listener lives in a tile);
 * [br.com.brunocarvalhs.howmuch.wear.CestouWearApplication] asks for a refresh whenever it changes.
 */
@AndroidEntryPoint
class LastListTileService : TileService() {

    @Inject
    lateinit var wearListStore: WearListStore

    @Inject
    lateinit var authService: AuthService

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> = scope.future {
        val summary = if (authService.currentUser != null) wearListStore.lastList() else null
        Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(Timeline.fromLayoutElement(layout(this@LastListTileService, summary)))
            .build()
    }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        scope.future { Resources.Builder().setVersion(RESOURCES_VERSION).build() }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun layout(context: Context, summary: WearListSummary?): LayoutElementBuilders.LayoutElement {
        val column = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        when {
            authService.currentUser == null -> column.addContent(hint(context, R.string.shopping_wear_tile_signed_out))
            summary == null -> column.addContent(hint(context, R.string.shopping_wear_tile_empty))
            else -> summary.lines(context).forEach { column.addContent(it) }
        }
        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(openAppModifier(context))
            .addContent(column.build())
            .build()
    }

    private fun WearListSummary.lines(context: Context): List<LayoutElementBuilders.LayoutElement> {
        val money = NumberFormat.getCurrencyInstance()
        val remaining = budget?.let { it - total }
        val pendingText = if (pending == 0) {
            context.getString(R.string.shopping_wear_all_done)
        } else {
            context.resources.getQuantityString(R.plurals.shopping_wear_pending, pending, pending)
        }
        return listOfNotNull(
            text("$emoji $title".trim(), SIZE_LABEL, COLOR_MUTED),
            text(money.format(total), SIZE_TOTAL, COLOR_TEXT),
            remaining?.let {
                val over = it < 0
                val label = if (over) R.string.shopping_wear_over_budget else R.string.shopping_wear_remaining
                text(context.getString(label, money.format(abs(it))), SIZE_DETAIL, if (over) COLOR_OVER else COLOR_OK)
            },
            text(pendingText, SIZE_DETAIL, COLOR_TEXT)
        )
    }

    private fun hint(context: Context, resId: Int) = text(context.getString(resId), SIZE_LABEL, COLOR_MUTED)

    private fun text(value: String, size: Float, color: Int) = LayoutElementBuilders.Text.Builder()
        .setText(value)
        .setMaxLines(1)
        .setFontStyle(
            LayoutElementBuilders.FontStyle.Builder()
                .setSize(sp(size))
                .setColor(argb(color))
                .build()
        )
        .build()

    private fun openAppModifier(context: Context) = ModifiersBuilders.Modifiers.Builder()
        .setClickable(
            ModifiersBuilders.Clickable.Builder()
                .setId("open_last_list")
                .setOnClick(
                    ActionBuilders.LaunchAction.Builder()
                        .setAndroidActivity(
                            ActionBuilders.AndroidActivity.Builder()
                                .setPackageName(context.packageName)
                                .setClassName(MainActivity::class.java.name)
                                .build()
                        )
                        .build()
                )
                .build()
        )
        .build()
}
