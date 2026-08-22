package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.components.SettingsHeader

@Composable
internal fun OpenSourceLicensesScreen(
    onBack: () -> Unit
) {
    val licenses = listOf(
        LicenseInfo("Android Jetpack", "Apache License 2.0"),
        LicenseInfo("Kotlin", "Apache License 2.0"),
        LicenseInfo("Firebase", "Google Terms of Service"),
        LicenseInfo("Hilt", "Apache License 2.0"),
        LicenseInfo("Ktor", "Apache License 2.0"),
        LicenseInfo("Coil", "Apache License 2.0"),
        LicenseInfo("Timber", "Apache License 2.0"),
        LicenseInfo("Material Components", "Apache License 2.0")
    )

    Scaffold(
        topBar = {
            SettingsHeader(
                title = "Licenças",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            items(licenses) { license ->
                ListItem(
                    headlineContent = { Text(license.library) },
                    supportingContent = { Text(license.license) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

data class LicenseInfo(val library: String, val license: String)
