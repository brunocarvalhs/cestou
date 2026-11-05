package br.com.brunocarvalhs.howmuch.feature.feedback.data.config

import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.RemoteVariableKeys
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.model.FeedbackConfig
import javax.inject.Inject

/** Lê flags e limiares do Remote Config a cada decisão, para o ajuste remoto valer sem nova versão. */
internal class FeedbackConfigSource @Inject constructor(
    private val featureFlags: FeatureFlagService,
    private val variables: RemoteVariableService
) {
    fun current() = FeedbackConfig(
        enabled = featureFlags.isEnabled(FeatureFlagKeys.FEEDBACK_ENABLED, true),
        storeReviewEnabled = featureFlags.isEnabled(FeatureFlagKeys.FEEDBACK_STORE_REVIEW_ENABLED, true),
        npsCooldownDays = variables.getLong(
            RemoteVariableKeys.FEEDBACK_NPS_COOLDOWN_DAYS,
            FeedbackConfig.DEFAULT_NPS_COOLDOWN_DAYS
        ),
        csatCooldownDays = variables.getLong(
            RemoteVariableKeys.FEEDBACK_CSAT_COOLDOWN_DAYS,
            FeedbackConfig.DEFAULT_CSAT_COOLDOWN_DAYS
        )
    )
}
