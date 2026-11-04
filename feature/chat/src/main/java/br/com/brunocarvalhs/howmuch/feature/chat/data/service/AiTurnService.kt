package br.com.brunocarvalhs.howmuch.feature.chat.data.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import br.com.brunocarvalhs.howmuch.feature.chat.data.notification.AiNotifier
import br.com.brunocarvalhs.howmuch.feature.chat.data.notification.RUNNING_NOTIFICATION_ID
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Serviço em primeiro plano que só existe para manter o processo vivo enquanto a IA executa um
 * pedido com o app fora de vista. O trabalho em si roda no scope de aplicação do chat; aqui só há
 * a notificação contínua. É ligado e desligado pelo [AiTurnServiceController].
 */
@AndroidEntryPoint
internal class AiTurnService : Service() {

    @Inject
    lateinit var notifier: AiNotifier

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notifier.runningNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(RUNNING_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else {
            startForeground(RUNNING_NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    /** Todo `shortService` tem um teto de tempo (cerca de 3 min); ao atingi-lo o serviço para. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }
}
