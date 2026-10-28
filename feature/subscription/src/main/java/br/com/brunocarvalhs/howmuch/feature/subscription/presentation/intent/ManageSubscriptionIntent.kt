package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.intent

data class ManageSubscriptionIntent(
    val onManageOnPlayStore: () -> Unit = {},
    val onBack: () -> Unit = {}
)
