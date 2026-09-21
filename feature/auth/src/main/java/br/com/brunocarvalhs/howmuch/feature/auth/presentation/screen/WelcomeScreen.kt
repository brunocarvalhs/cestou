package br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.brunocarvalhs.howmuch.core.common.extensions.openBrowser
import br.com.brunocarvalhs.howmuch.core.common.util.LegalUrls
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.core.ui.extensions.currentAppLocaleTag
import br.com.brunocarvalhs.howmuch.core.ui.extensions.supportedLanguages
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
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val languageSheetState = rememberModalBottomSheetState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("welcome_logo"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cestou_logomark),
                        contentDescription = stringResource(R.string.welcome_logo_content_description),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = stringResource(R.string.welcome_brand_name),
                    modifier = Modifier.testTag("welcome_brand_name"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            WelcomeIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.welcome_title),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                WelcomeLegalNotice()

                Spacer(modifier = Modifier.height(24.dp))

                LanguageSelector(
                    modifier = Modifier.height(42.dp),
                    onClick = { showLanguageSheet = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (showLanguageSheet) {
                    val context = LocalContext.current
                    val currentLocale = context.currentAppLocaleTag()
                    val languages = context.supportedLanguages()

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

                            languages.forEach { (language, code) ->
                                val isSelected = code == currentLocale

                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = language,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.Unspecified
                                            }
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
                                        intent.onLanguageSelected(code)
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
                        .height(58.dp)
                        .testTag("welcome_agree_and_continue_button"),
                    shape = RoundedCornerShape(32.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.welcome_footer, state.version),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("welcome_footer"),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        modifier = modifier.testTag("welcome_hero_image"),
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
private fun WelcomeLegalNotice() {
    val context = LocalContext.current
    val bodyStyle = MaterialTheme.typography.bodyLarge
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .testTag("welcome_legal_notice"),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.welcome_legal_notice_before_privacy),
            style = bodyStyle,
            color = bodyColor,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.auth_privacy_policy),
            style = bodyStyle,
            color = linkColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .testTag("welcome_legal_notice_privacy_policy_link")
                .clickable { context.openBrowser(LegalUrls.PRIVACY_POLICY_URL) }
        )
        Text(
            text = stringResource(
                R.string.welcome_legal_notice_middle,
                stringResource(R.string.welcome_agree_and_continue)
            ),
            style = bodyStyle,
            color = bodyColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.welcome_legal_notice_terms_of_service),
            style = bodyStyle,
            color = linkColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .testTag("welcome_legal_notice_terms_of_service_link")
                .clickable { context.openBrowser(LegalUrls.TERMS_OF_USE_URL) }
        )
        Text(
            text = stringResource(R.string.auth_terms_suffix),
            style = bodyStyle,
            color = bodyColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LanguageSelector(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val currentLocale = context.currentAppLocaleTag()
    val languages = context.supportedLanguages()

    val currentLanguageName = languages.firstOrNull { (_, code) -> code == currentLocale }
        ?.first
        ?: "English"

    Button(
        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.surfaceContainerLow),
        onClick = {
            onClick()
        }
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = "Language",
            modifier = Modifier.size(30.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = currentLanguageName,
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(24.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Select language",
            modifier = Modifier.size(30.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@PreviewCestouScreens
@Composable
private fun WelcomeScreenPreview() {
    CestouTheme {
        WelcomeScreen(state = WelcomeUiState(version = "1.3.0")) {
            CestouButton(
                text = "Começar",
                onClick = { },
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
            )
        }
    }
}
