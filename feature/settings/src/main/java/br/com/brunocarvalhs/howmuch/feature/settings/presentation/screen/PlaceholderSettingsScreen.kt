package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.components.SettingsHeader

@Composable
internal fun PlaceholderSettingsScreen(
    title: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            SettingsHeader(
                title = title,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Funcionalidade de $title em breve")
        }
    }
}
