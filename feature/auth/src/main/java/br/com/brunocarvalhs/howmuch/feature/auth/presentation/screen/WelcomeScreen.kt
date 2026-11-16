package br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
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
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButtonPrimary
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouModalBottomSheet
import br.com.brunocarvalhs.howmuch.core.ui.extensions.currentAppLocaleTag
import br.com.brunocarvalhs.howmuch.core.ui.extensions.supportedLanguages
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.WelcomeIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.WelcomeUiState
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WelcomeScreen(
    state: WelcomeUiState,
    intent: WelcomeIntent = WelcomeIntent(),
    illustrationModel: Any? = null,
    actions: @Composable ColumnScope.() -> Unit,
) {
    var showLanguageSheet by remember { mutableStateOf(value = false) }
    var showSheet by remember { mutableStateOf(value = false) }
    val sheetState = rememberModalBottomSheetState()
    val languageSheetState = rememberModalBottomSheetState()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1D51D8),
            Color(0xFF143BA2),
            Color(0xFF0F2C80),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                val isLandscape = (maxWidth > maxHeight) || (maxWidth >= 600.dp)

                if (isLandscape) {
                    WelcomeHorizontalContent(
                        version = state.version,
                        illustrationModel = illustrationModel,
                        onLanguageClick = { showLanguageSheet = true },
                        onAccessClick = { showSheet = true },
                    )
                } else {
                    WelcomeVerticalContent(
                        version = state.version,
                        illustrationModel = illustrationModel,
                        onLanguageClick = { showLanguageSheet = true },
                        onAccessClick = { showSheet = true },
                    )
                }
            }
        }

        if (showLanguageSheet) {
            val context = LocalContext.current
            val currentLocale = context.currentAppLocaleTag()
            val languages = context.supportedLanguages()

            CestouModalBottomSheet(
                onDismissRequest = { showLanguageSheet = false },
                sheetState = languageSheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                ) {
                    Text(
                        text = stringResource(R.string.welcome_select_language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(24.dp),
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
                                    },
                                )
                            },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                intent.onLanguageSelected(code)
                                showLanguageSheet = false
                            },
                        )
                    }
                }
            }
        }

        if (showSheet) {
            CestouModalBottomSheet(
                onDismissRequest = {
                    showSheet = false
                },
                sheetState = sheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    actions(this)
                }
            }
        }
    }
}

@Composable
private fun WelcomeVerticalContent(
    version: String,
    illustrationModel: Any?,
    onLanguageClick: () -> Unit,
    onAccessClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageCount = 3
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            WelcomeHeader(
                onLanguageClick = onLanguageClick,
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AsyncImage(
                        model = illustrationModel,
                        contentDescription = stringResource(R.string.welcome_logo_content_description),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .testTag("welcome_hero_image"),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = when (page) {
                            1 -> stringResource(R.string.welcome_title_page_2)
                            2 -> stringResource(R.string.welcome_title_page_3)
                            else -> stringResource(R.string.welcome_title)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = when (page) {
                            1 -> stringResource(R.string.welcome_description_page_2)
                            2 -> stringResource(R.string.welcome_description_page_3)
                            else -> stringResource(R.string.welcome_description)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.85f),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    WelcomeLegalNotice()
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            PageIndicatorDots(
                currentPage = pagerState.currentPage,
                pageCount = pageCount,
            )

            Spacer(modifier = Modifier.height(20.dp))

            WelcomeAccessButton(
                onClick = onAccessClick,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.welcome_footer, version),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_footer"),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun WelcomeHorizontalContent(
    version: String,
    illustrationModel: Any?,
    onLanguageClick: () -> Unit,
    onAccessClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageCount = 3
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                WelcomeHeader(
                    onLanguageClick = onLanguageClick,
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { page ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = when (page) {
                                1 -> stringResource(R.string.welcome_title_page_2)
                                2 -> stringResource(R.string.welcome_title_page_3)
                                else -> stringResource(R.string.welcome_title)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = when (page) {
                                1 -> stringResource(R.string.welcome_description_page_2)
                                2 -> stringResource(R.string.welcome_description_page_3)
                                else -> stringResource(R.string.welcome_description)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.85f),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        WelcomeLegalNotice()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PageIndicatorDots(
                    currentPage = pagerState.currentPage,
                    pageCount = pageCount,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            WelcomeAccessButton(
                onClick = onAccessClick,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.welcome_footer, version),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_footer"),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.5f),
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = illustrationModel,
                contentDescription = stringResource(R.string.welcome_logo_content_description),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("welcome_hero_image"),
            )
        }
    }
}

@Composable
private fun WelcomeHeader(
    onLanguageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .testTag("welcome_logo"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cestou_logomark),
                    contentDescription = stringResource(R.string.welcome_logo_content_description),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = stringResource(R.string.welcome_brand_name),
                modifier = Modifier.testTag("welcome_brand_name"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .clickable(onClick = onLanguageClick)
                .testTag("welcome_language_button"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = stringResource(R.string.welcome_language_content_description),
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun WelcomeLegalNotice() {
    val context = LocalContext.current
    val bodyStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
    val bodyColor = Color.White.copy(alpha = 0.8f)
    val linkColor = Color.White

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("welcome_legal_notice"),
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text = stringResource(R.string.auth_terms_prefix),
            style = bodyStyle,
            color = bodyColor,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.auth_terms_of_use),
            style = bodyStyle,
            color = linkColor,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .testTag("welcome_legal_notice_terms_of_service_link")
                .clickable { context.openBrowser(LegalUrls.TERMS_OF_USE_URL) },
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.auth_terms_and),
            style = bodyStyle,
            color = bodyColor,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.auth_privacy_policy),
            style = bodyStyle,
            color = linkColor,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .testTag("welcome_legal_notice_privacy_policy_link")
                .clickable { context.openBrowser(LegalUrls.PRIVACY_POLICY_URL) },
        )
        Text(
            text = stringResource(R.string.auth_terms_suffix),
            style = bodyStyle,
            color = bodyColor,
        )
    }
}

@Composable
private fun PageIndicatorDots(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            if (index == currentPage) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.45f)),
                )
            }
        }
    }
}

@Composable
private fun WelcomeAccessButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CestouButtonPrimary(
        text = stringResource(R.string.welcome_agree_and_continue),
        onClick = onClick,
        modifier = modifier.testTag("welcome_agree_and_continue_button"),
        leadingIcon = Icons.AutoMirrored.Filled.Login,
        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        containerColor = Color.White,
        contentColor = Color(0xFF1D51D8),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
        ),
    )
}

@PreviewCestouScreens
@Composable
private fun WelcomeScreenPreview() {
    CestouTheme {
        WelcomeScreen(state = WelcomeUiState(version = "1.3.0")) {
            CestouButtonPrimary(
                text = "Começar",
                onClick = { },
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            )
        }
    }
}
