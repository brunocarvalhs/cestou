package br.com.brunocarvalhs.howmuch.feature.chat.data.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.navigation.AiChatLaunch
import br.com.brunocarvalhs.howmuch.feature.chat.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val RESULTS_CHANNEL_ID = "ai_actions"
private const val RUNNING_CHANNEL_ID = "ai_running"
private const val RESULT_NOTIFICATION_ID = 7101
private const val APPROVAL_NOTIFICATION_ID = 7102
internal const val RUNNING_NOTIFICATION_ID = 7100
private const val MAX_RESULT_LENGTH = 240

/**
 * Notificações da IA: a contínua do serviço, a de resultado e a de "precisa da sua aprovação".
 * Só notifica com o interruptor de notificações do app ligado, a permissão concedida e o canal
 * não bloqueado; sem isso o pedido segue e a resposta fica no chat.
 */
@Singleton
internal class AiNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val manager get() = NotificationManagerCompat.from(context)

    /** Notificação contínua do serviço em primeiro plano; discreta, sem som. */
    fun runningNotification(): Notification {
        ensureChannels()
        return NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(context.getString(R.string.ai_notification_running_title))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** A mensagem da própria IA; na tela bloqueada aparece só uma versão genérica. */
    suspend fun postResult(sessionId: String?, text: String) {
        if (!canNotify()) return
        val body = text.trim().take(MAX_RESULT_LENGTH)
        notify(
            id = RESULT_NOTIFICATION_ID,
            title = context.getString(R.string.ai_notification_result_title),
            text = body,
            publicText = context.getString(R.string.ai_notification_result_public),
            sessionId = sessionId
        )
    }

    suspend fun postApprovalNeeded(sessionId: String?) {
        if (!canNotify()) return
        notify(
            id = APPROVAL_NOTIFICATION_ID,
            title = context.getString(R.string.ai_notification_approval_title),
            text = context.getString(R.string.ai_notification_approval_text),
            publicText = context.getString(R.string.ai_notification_approval_title),
            sessionId = sessionId
        )
    }

    fun cancelAll() {
        manager.cancel(RESULT_NOTIFICATION_ID)
        manager.cancel(APPROVAL_NOTIFICATION_ID)
    }

    private suspend fun canNotify(): Boolean {
        val enabledInApp = settingsRepository.getSettings().first().notificationsEnabled
        // areNotificationsEnabled() já considera a permissão POST_NOTIFICATIONS no Android 13+.
        val allowed = enabledInApp && manager.areNotificationsEnabled() && !isChannelBlocked()
        if (!allowed) Timber.d("AI notification skipped: not allowed in the app")
        return allowed
    }

    private fun isChannelBlocked(): Boolean {
        ensureChannels()
        val channel = manager.getNotificationChannel(RESULTS_CHANNEL_ID) ?: return false
        return channel.importance == NotificationManager.IMPORTANCE_NONE
    }

    private fun notify(id: Int, title: String, text: String, publicText: String, sessionId: String?) {
        val publicVersion = NotificationCompat.Builder(context, RESULTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(publicText)
            .build()
        val notification = NotificationCompat.Builder(context, RESULTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openChatIntent(id, sessionId))
            .build()
        // A permissão é conferida aqui, no mesmo método que notifica, que é o que o lint reconhece.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.notify(id, notification)
    }

    /** Abre o app já na conversa pelo deeplink `cestou://chat/<id>`; sem id, só abre o app. */
    private fun openChatIntent(requestCode: Int, sessionId: String?): PendingIntent? {
        val launch = if (sessionId != null) {
            Intent(Intent.ACTION_VIEW, AiChatLaunch.uriFor(sessionId)).setPackage(context.packageName)
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        }
        launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            context,
            requestCode,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val system = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        system.createNotificationChannel(
            NotificationChannel(
                RESULTS_CHANNEL_ID,
                context.getString(R.string.ai_notification_channel_results),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        system.createNotificationChannel(
            NotificationChannel(
                RUNNING_CHANNEL_ID,
                context.getString(R.string.ai_notification_channel_running),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }
}
