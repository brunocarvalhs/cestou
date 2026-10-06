package br.com.brunocarvalhs.howmuch.core.domain.model

/**
 * A user's Pro entitlement, as seen by anything that gates behaviour on it (paid features'
 * MVI `State`, the AI agent dispatch chokepoint). Deliberately just two states with no payload:
 * no purchase token, no expiry timestamp, no Play Billing type. Those live in whatever backs
 * [br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository]
 * (`core:billing`'s `BillingClient` wrapper) and stay there - see AD-010 in `.specs/STATE.md`.
 */
enum class SubscriptionStatus {
    FREE,
    PRO
}
