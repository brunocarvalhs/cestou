package br.com.brunocarvalhs.howmuch.update

import android.content.IntentSender
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import com.google.android.gms.common.api.ApiException
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Envolve o [AppUpdateManager] do Play para o force update (sdd-059). Só consulta o Play quando a
 * versão instalada está abaixo do mínimo remoto, e nunca quando o mínimo é zero.
 *
 * Falhas do Play são registradas como não fatais e não bloqueiam o app: sem resposta, o app segue
 * normalmente (fail-open) em vez de travar o usuário por um erro de infraestrutura.
 */
internal class ForceUpdateController @Inject constructor(
    private val appUpdateManager: AppUpdateManager,
    private val crashReporter: CrashReporter
) {

    /** Última informação de atualização que o Play devolveu, usada para abrir o fluxo IMMEDIATE. */
    private var pendingUpdate: AppUpdateInfo? = null

    /**
     * Decide o estado do force update. [minVersionCode] é o mínimo remoto e [installedVersionCode]
     * o versionCode em execução.
     */
    suspend fun check(minVersionCode: Long, installedVersionCode: Long): ForceUpdateState {
        if (minVersionCode <= 0L || installedVersionCode >= minVersionCode) return ForceUpdateState.Idle
        return try {
            val info = appUpdateManager.appUpdateInfo.await()
            pendingUpdate = info
            when {
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    ForceUpdateState.Required(resume = true)

                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                    ForceUpdateState.Required(resume = false)

                else -> ForceUpdateState.Blocked
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            crashReporter.recordException(e)
            ForceUpdateState.Idle
        }
    }

    /**
     * Abre a tela do Play para a atualização IMMEDIATE, usando a informação da última checagem.
     * Devolve `false` quando não há atualização conhecida para abrir.
     */
    fun startImmediateUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        val info = pendingUpdate ?: return false
        return try {
            appUpdateManager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)
            )
            true
        } catch (e: IntentSender.SendIntentException) {
            crashReporter.recordException(e)
            false
        }
    }
}
