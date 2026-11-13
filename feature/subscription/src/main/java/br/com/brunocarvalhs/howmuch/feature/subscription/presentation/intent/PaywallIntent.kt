package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.intent

import android.app.Activity

data class PaywallIntent(
    val onSubscribeClick: (Activity) -> Unit = {},
    val onPlanSelected: (String) -> Unit = {},
    val onBack: () -> Unit = {}
)
