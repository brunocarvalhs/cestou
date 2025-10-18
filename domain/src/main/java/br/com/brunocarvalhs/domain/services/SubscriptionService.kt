package br.com.brunocarvalhs.domain.services

import android.app.Activity
import br.com.brunocarvalhs.domain.entities.PlanSubscription

interface SubscriptionService {
    suspend fun isUserPremium(): Boolean
    suspend fun launchPremiumPurchase(activity: Activity): Result<Unit>
    suspend fun listPlans(): List<PlanSubscription>
    suspend fun cancelSubscription(activity: Activity): Result<Unit>
}
