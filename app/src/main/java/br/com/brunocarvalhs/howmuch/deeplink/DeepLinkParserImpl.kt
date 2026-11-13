package br.com.brunocarvalhs.howmuch.deeplink

import android.content.Intent
import android.net.Uri
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.common.util.InviteLink
import br.com.brunocarvalhs.howmuch.core.navigation.AiChatLaunch
import javax.inject.Inject

internal class DeepLinkParserImpl @Inject constructor(
    private val analyticsTracker: AnalyticsTracker
) : DeepLinkParser {

    override fun parse(intent: Intent): DeepLinkAction {
        val data = intent.data.takeIf { intent.action == Intent.ACTION_VIEW } ?: return DeepLinkAction.Ignored
        return pairWatch(data)
            ?: shoppingCreate(data)
            ?: subscription(data)
            ?: aiChat(data)
            ?: invite(data)
            ?: DeepLinkAction.Ignored
    }

    private fun pairWatch(data: Uri): DeepLinkAction? =
        WearPairingLink.parse(data)?.let {
            track("wear_pairing")
            DeepLinkAction.PairWatch(it)
        }

    private fun shoppingCreate(data: Uri): DeepLinkAction? {
        if (!ShoppingCreateLink.matches(data)) return null
        track("shopping_create")
        return DeepLinkAction.ShoppingCreate
    }

    private fun subscription(data: Uri): DeepLinkAction? {
        if (!SubscriptionLink.matches(data)) return null
        track("wear_subscription")
        return DeepLinkAction.Subscription
    }

    private fun aiChat(data: Uri): DeepLinkAction? =
        AiChatLaunch.sessionIdFrom(data)?.let {
            track("ai_chat")
            DeepLinkAction.AiChat(it)
        }

    private fun invite(data: Uri): DeepLinkAction? =
        if (InviteLink.matches(data)) {
            InviteLink.tokenFrom(data)?.let {
                track("invite")
                DeepLinkAction.Invite(it)
            }
        } else {
            null
        }

    private fun track(source: String) {
        analyticsTracker.trackEvent(AnalyticsEvents.DEEP_LINK_OPENED, mapOf(AnalyticsParams.SOURCE to source))
    }
}
