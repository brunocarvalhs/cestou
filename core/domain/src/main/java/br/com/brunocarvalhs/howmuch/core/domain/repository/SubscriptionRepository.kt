package br.com.brunocarvalhs.howmuch.core.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Read-only entitlement contract (AD-010). This is the *only* thing a paid feature or the AI
 * agent dispatch chokepoint is allowed to depend on to ask "is this user Pro" - never a
 * Play Billing type, never an `Activity`. The actual `BillingClient` wrapper that keeps this
 * up to date lives in `core:billing`, isolated there because it carries a third-party
 * dependency and an `Activity`-scoped purchase flow that this contract deliberately excludes.
 */
interface SubscriptionRepository {
    fun observeStatus(): Flow<SubscriptionStatus>
}
