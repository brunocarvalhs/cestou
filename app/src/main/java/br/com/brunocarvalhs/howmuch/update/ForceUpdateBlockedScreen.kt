package br.com.brunocarvalhs.howmuch.update

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton

/**
 * Tela de bloqueio do force update (sdd-059). Aparece quando a versão está abaixo do mínimo e o
 * Play não oferece atualização (ex.: build instalada fora da Play). Leva à página do app na Play Store.
 */
@Composable
internal fun ForceUpdateBlockedScreen() {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("force_update_blocked"),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.force_update_blocked_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.force_update_blocked_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
            )
            CestouButton(
                text = stringResource(R.string.force_update_blocked_button),
                onClick = {
                    val packageName = context.packageName
                    val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                    // Sem a Play Store instalada, abre a página na web em vez de falhar.
                    runCatching { context.startActivity(storeIntent) }
                        .onFailure { context.startActivity(webIntent) }
                },
                modifier = Modifier.testTag("force_update_store_button")
            )
        }
    }
}
