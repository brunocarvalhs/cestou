package br.com.brunocarvalhs.howmuch.feature.chat.data.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Liga o [AiTurnService] no primeiro turno em andamento e o desliga quando o último termina. É
 * chamado com o app em primeiro plano (o usuário acabou de enviar a mensagem), o que o Android
 * exige para iniciar um serviço em primeiro plano; se mesmo assim for recusado, o turno segue sem
 * o serviço, só sem a garantia de sobreviver ao app fechar.
 */
@Singleton
internal class AiTurnServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val activeTurns = AtomicInteger(0)

    fun turnStarted() {
        if (activeTurns.getAndIncrement() == 0) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, AiTurnService::class.java))
            } catch (e: IllegalStateException) {
                Timber.w(e, "Could not start the AI foreground service")
            } catch (e: SecurityException) {
                Timber.w(e, "Could not start the AI foreground service")
            }
        }
    }

    fun turnFinished() {
        if (activeTurns.updateAndGet { (it - 1).coerceAtLeast(0) } == 0) {
            context.stopService(Intent(context, AiTurnService::class.java))
        }
    }
}
