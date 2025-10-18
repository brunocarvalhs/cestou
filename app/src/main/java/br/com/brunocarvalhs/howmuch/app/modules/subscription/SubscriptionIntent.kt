package br.com.brunocarvalhs.howmuch.app.modules.subscription

import android.app.Activity

sealed interface SubscriptionIntent {
    data class Subscribe(val activity: Activity) : SubscriptionIntent
    object Cancel : SubscriptionIntent
}
