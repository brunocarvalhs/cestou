package br.com.brunocarvalhs.howmuch.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Meta de gasto mensal do usuário logado, sincronizada na conta (Firestore) para aparecer em
 * qualquer aparelho. Fica num documento privado, nunca em `users/{uid}`, que qualquer usuário
 * logado consegue ler.
 */
interface MonthlyGoalRepository {

    /** Meta atual; `null` sem meta ou sem sessão. Emite a partir do cache offline do Firestore. */
    fun observeGoal(): Flow<Double?>

    /** Grava a meta; `null` ou valor menor ou igual a zero remove. */
    suspend fun setGoal(goal: Double?): Result<Unit>
}
