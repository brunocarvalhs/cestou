package br.com.brunocarvalhs.howmuch.core.data.repository

import br.com.brunocarvalhs.howmuch.core.data.model.BudgetPreferencesModel
import br.com.brunocarvalhs.howmuch.core.domain.repository.MonthlyGoalRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.services.make
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A meta fica em `users/{uid}/preferences/budget`, subcoleção privada do dono: `users/{uid}` é
 * legível por qualquer usuário logado (AD-009) e a meta é dado financeiro.
 */
@Singleton
class MonthlyGoalRepositoryImpl @Inject constructor(
    private val networkService: NetworkService,
    private val authService: AuthService
) : MonthlyGoalRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeGoal(): Flow<Double?> = authService.authState
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { uid -> if (uid == null) flowOf(null) else observeFor(uid) }
        .distinctUntilChanged()

    private fun observeFor(uid: String): Flow<Double?> = networkService.observe<BudgetPreferencesModel>(
        request = NetworkService.NetworkRequest(
            endpoint = "$USERS/$uid/$PREFERENCES/$BUDGET_DOC",
            method = NetworkService.Method.GET
        )
    ).map { it?.monthlyGoal?.takeIf { goal -> goal > 0.0 } }
        .catch { e ->
            Timber.e(e, "Error observing the monthly goal")
            emit(null)
        }

    // POST com id vira set() (cria ou substitui o documento). make() devolve null quando a rede
    // falha, então null é tratado como erro para o chamador não achar que gravou.
    override suspend fun setGoal(goal: Double?): Result<Unit> = runCatching {
        val uid = checkNotNull(authService.currentUser?.id) { "No signed-in user" }
        val saved = networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = "$USERS/$uid/$PREFERENCES",
                method = NetworkService.Method.POST,
                payload = mapOf("id" to BUDGET_DOC, "monthlyGoal" to goal?.takeIf { it > 0.0 })
            ),
            response = String::class
        )
        checkNotNull(saved) { "Monthly goal was not saved" }
        Unit
    }

    private companion object {
        const val USERS = "users"
        const val PREFERENCES = "preferences"
        const val BUDGET_DOC = "budget"
    }
}
