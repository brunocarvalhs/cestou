package br.com.brunocarvalhs.howmuch.core.domain.services

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping

/**
 * AD-011: signature-only port so `core/domain` stays framework-free (no `Context`/`Intent`).
 * Implemented in `feature/products`, where the Android share `Intent` is actually built.
 */
interface ShareShoppingUseCase {
    suspend operator fun invoke(shopping: Shopping)
}
