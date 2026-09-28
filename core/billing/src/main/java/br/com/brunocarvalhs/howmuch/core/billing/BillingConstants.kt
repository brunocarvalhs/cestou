package br.com.brunocarvalhs.howmuch.core.billing

/**
 * Play Console product identifiers for the Pro subscription (AD-010).
 *
 * [PRO_MONTHLY] is a placeholder — ajustar para bater com o ID real do produto quando bruno
 * criar a assinatura no Play Console. Isso ainda está pendente ("blocked on bruno for the
 * commercial half" em AD-010) e não bloqueia este módulo: nenhum preço é hardcoded em código,
 * o valor exibido ao usuário vem sempre de [com.android.billingclient.api.ProductDetails] via
 * `queryProductDetails`.
 */
object BillingConstants {
    const val PRO_MONTHLY = "pro_monthly"
}
