package br.com.brunocarvalhs.howmuch.app.foundation.analytics

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.navigation.NavController

fun Context.openApp() {
    AnalyticsEvents.trackEvent(
        AnalyticsEvent.APP_OPEN,
        mapOf(
            AnalyticsParam.TIMESTAMP to System.currentTimeMillis(),
            AnalyticsParam.APP_VERSION to packageManager.getPackageInfo(
                packageName,
                0
            ).versionName.orEmpty(),
            AnalyticsParam.DEVICE to Build.MODEL,
            AnalyticsParam.OS_VERSION to Build.VERSION.RELEASE
        )
    )
}

fun Activity.firstOpen() {
    AnalyticsEvents.trackEvent(
        AnalyticsEvent.FIRST_OPEN,
        mapOf(
            AnalyticsParam.TIMESTAMP to System.currentTimeMillis(),
            AnalyticsParam.APP_VERSION to packageManager.getPackageInfo(
                packageName,
                0
            ).versionName.orEmpty(),
            AnalyticsParam.DEVICE to Build.MODEL,
            AnalyticsParam.OS_VERSION to Build.VERSION.RELEASE
        )
    )
}

fun NavController.trackNavigation() {
    addOnDestinationChangedListener { _, destination, _ ->
        AnalyticsEvents.trackScreen(destination.route.toString())
    }
}

internal fun trackClick(
    viewId: String,
    viewName: String? = null,
    screenName: String? = null
) = {
    AnalyticsEvents.trackEvent(
        AnalyticsEvent.CLICK,
        mapOf(
            AnalyticsParam.ITEM_ID to viewId,
            AnalyticsParam.ITEM_NAME to (viewName ?: viewId),
            AnalyticsParam.SCREEN_NAME to (screenName ?: "Unknown"),
            AnalyticsParam.TIMESTAMP to System.currentTimeMillis()
        )
    )
}
