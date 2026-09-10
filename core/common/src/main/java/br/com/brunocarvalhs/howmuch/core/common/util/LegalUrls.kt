package br.com.brunocarvalhs.howmuch.core.common.util

/**
 * Hosted legal document URLs for Cestou (How Much).
 *
 * Centralized here so the Privacy Policy / Terms of Use links are only hardcoded once and can be
 * reused by every feature module that needs to open them (auth onboarding, settings, etc.).
 */
object LegalUrls {
    const val PRIVACY_POLICY_URL = "https://bruno-carvalho.dev.br/legal?doc=cestou-privacy-policy"
    const val TERMS_OF_USE_URL = "https://bruno-carvalho.dev.br/legal?doc=cestou-terms-of-use"
}
