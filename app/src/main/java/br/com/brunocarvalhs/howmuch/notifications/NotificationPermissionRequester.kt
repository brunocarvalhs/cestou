package br.com.brunocarvalhs.howmuch.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

private const val PREFS = "cestou_notifications"
private const val KEY_ASKED = "permission_asked"

/**
 * Decide quando pedir a permissão de notificações: no Android 13+, uma vez por instalação, só
 * depois do login (nunca na tela de boas-vindas). Sem ela os lembretes de compras e os avisos da
 * IA não aparecem; recusar não bloqueia nenhuma função.
 */
internal class NotificationPermissionRequester(
    private val context: Context,
    private val sdkInt: Int = Build.VERSION.SDK_INT
) {

    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun shouldAsk(): Boolean {
        if (sdkInt < Build.VERSION_CODES.TIRAMISU) return false
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return !granted && !prefs.getBoolean(KEY_ASKED, false)
    }

    /** Marca como pedido antes de mostrar o diálogo: se o app fechar no meio, não pergunta de novo. */
    fun markAsked() {
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
    }
}
