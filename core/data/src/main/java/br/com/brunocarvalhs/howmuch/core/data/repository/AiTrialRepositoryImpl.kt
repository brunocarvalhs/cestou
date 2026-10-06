package br.com.brunocarvalhs.howmuch.core.data.repository

import br.com.brunocarvalhs.howmuch.core.data.di.AiTrialDataStore
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import br.com.brunocarvalhs.howmuch.core.domain.services.get
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiTrialRepositoryImpl @Inject constructor(
    @AiTrialDataStore private val storageService: StorageService
) : AiTrialRepository {

    override fun hasUsedFreeMessage(): Flow<Boolean> =
        storageService.observe<Boolean>(KEY_FREE_MESSAGE_USED).map { it ?: false }

    override suspend fun markFreeMessageUsed() {
        // NonCancellable: AiChatViewModel calls this fire-and-forget from viewModelScope.launch,
        // which is cancelled the instant the chat screen closes. Without this, a write still in
        // flight when the user backs out quickly was silently dropped — the trial-used flag
        // never reached disk, so reopening the app "refunded" the free message.
        withContext(NonCancellable) {
            storageService.save(KEY_FREE_MESSAGE_USED, true)
        }
    }

    private companion object {
        const val KEY_FREE_MESSAGE_USED = "ai_free_message_used"
    }
}
