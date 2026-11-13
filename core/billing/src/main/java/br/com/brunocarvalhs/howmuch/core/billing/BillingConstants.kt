package br.com.brunocarvalhs.howmuch.core.billing

/**
 * Play Console identifiers for the Pro subscription (AD-010, sdd-059).
 *
 * Um único produto ([PRO_MONTHLY]) com um base plan por período. Os base plans são o que
 * diferencia mensal, semestral e anual. Assim o SKU do gerenciamento de assinatura e o entitlement
 * permanecem os mesmos para todos os planos.
 *
 * O valor exibido ao usuário vem sempre de [com.android.billingclient.api.ProductDetails] via
 * `queryProductDetails`. Nenhum preço é hardcoded em código.
 */
object BillingConstants {
    const val PRO_MONTHLY = "pro_monthly"

    const val BASE_PLAN_MONTHLY = "monthly"
    const val BASE_PLAN_SEMIANNUAL = "semiannual"
    const val BASE_PLAN_ANNUAL = "annual"

    /** Base plans conhecidos, na ordem em que o paywall os lista. Base plans fora desta lista são ignorados. */
    val KNOWN_BASE_PLANS: List<String> = listOf(BASE_PLAN_MONTHLY, BASE_PLAN_SEMIANNUAL, BASE_PLAN_ANNUAL)
}
