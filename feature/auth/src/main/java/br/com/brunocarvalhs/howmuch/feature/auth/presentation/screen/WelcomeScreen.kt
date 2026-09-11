package br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.extensions.systemLanguageTag
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.WelcomeIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.WelcomeUiState
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WelcomeScreen(
    state: WelcomeUiState,
    intent: WelcomeIntent = WelcomeIntent(),
    actions: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            WelcomeIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 24.dp
                    ).padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.welcome_title),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.welcome_description),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))

                var showLanguageSheet by remember { mutableStateOf(false) }
                LanguageSelector(onClick = { showLanguageSheet = true })

                Spacer(modifier = Modifier.weight(1f))

                var showSheet by remember { mutableStateOf(false) }
                val sheetState = rememberModalBottomSheetState()
                val languageSheetState = rememberModalBottomSheetState()

                if (showLanguageSheet) {
                    val context = LocalContext.current
                    val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
                        ?: context.systemLanguageTag()
                    val languages = context.resources.getStringArray(br.com.brunocarvalhs.howmuch.core.ui.R.array.supported_languages)
                    val languageCodes = context.resources.getStringArray(br.com.brunocarvalhs.howmuch.core.ui.R.array.supported_languages_codes)

                    ModalBottomSheet(
                        onDismissRequest = { showLanguageSheet = false },
                        sheetState = languageSheetState,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.welcome_select_language),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(24.dp)
                            )
                            
                            HorizontalDivider()

                            languages.forEachIndexed { index, language ->
                                val isSelected = languageCodes[index] == currentLocale
                                
                                ListItem(
                                    headlineContent = { 
                                        Text(
                                            text = language,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified
                                        ) 
                                    },
                                    trailingContent = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        intent.onLanguageSelected(languageCodes[index])
                                        showLanguageSheet = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        showSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(32.dp),
                ) {
                    Text(
                        text = stringResource(R.string.welcome_agree_and_continue),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showSheet = false
                        },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        // Sheet content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            actions(this)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WelcomeIllustration(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.welcome_animation)
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun LanguageSelector(onClick: () -> Unit) {
    val context = LocalContext.current
    val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
        ?: context.systemLanguageTag()
    
    val languages = context.resources.getStringArray(br.com.brunocarvalhs.howmuch.core.ui.R.array.supported_languages)
    val languageCodes = context.resources.getStringArray(br.com.brunocarvalhs.howmuch.core.ui.R.array.supported_languages_codes)
    
    val currentLanguageName = languageCodes.indexOf(currentLocale).let { index ->
        if (index != -1) languages[index] else "English"
    }

    Surface(
        modifier = Modifier
            .width(320.dp)
            .height(58.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Language",
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(28.dp))

            Text(
                text = currentLanguageName,
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(55.dp))

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select language",
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    MaterialTheme {
        WelcomeScreen(state = WelcomeUiState(version = "1.2.0")) {
            CestouButton(
                text = "Começa",
                onClick = {  },
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
            )
        }
    }
}
