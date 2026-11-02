package br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.component.AiChatUpgradeBanner
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.component.AnimatedColorText
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.component.ChatBubble
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.component.TypingBubble
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import timber.log.Timber
import java.util.Locale
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreUiR

private const val LEADING_AND_TRAILING_SPACERS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    state: AiChatUiState,
    intent: AiChatIntent,
    isSendEnabled: Boolean = true,
    onUpgradeClick: () -> Unit = {},
) {
    var isInputFocused by remember { mutableStateOf(value = false) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                intent.onVoiceResultReceived(spokenText)
                intent.onSendMessage()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            val speechIntent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH,
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault().toLanguageTag(),
                )
            }
            runCatching {
                speechRecognizerLauncher.launch(speechIntent)
            }.onFailure { e ->
                Timber.e(e, "Failed to launch speech intent after permission")
            }
        }
    }

    val isChatActive by rememberUpdatedState(
        newValue = state.messages.isNotEmpty() || isInputFocused,
    )

    val animatedHeight by animateFloatAsState(
        targetValue = if (isChatActive) 1f else 0.4f,
        label = "bottomSheetHeight",
    )

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { newState ->
            !(isChatActive && (newState != SheetValue.Expanded))
        },
    )

    LaunchedEffect(isChatActive) {
        if (isChatActive && sheetState.currentValue != SheetValue.Expanded) {
            sheetState.expand()
        }
    }

    BackHandler(enabled = isChatActive) {
        intent.onBack()
    }

    ModalBottomSheet(
        onDismissRequest = intent.onBack,
        sheetState = sheetState,
        sheetGesturesEnabled = !isChatActive,
        dragHandle = {
            if (!isChatActive) {
                BottomSheetDefaults.DragHandle()
            }
        },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedHeight),
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        title = { Text(text = stringResource(R.string.ai_chat_title)) },
                        navigationIcon = {
                            IconButton(onClick = intent.onToggleHistory) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = stringResource(R.string.ai_chat_history_content_description),
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = intent.onSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.ai_chat_settings_content_description),
                                )
                            }
                            IconButton(onClick = intent.onBack) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.ai_chat_close_content_description),
                                )
                            }
                        },
                    )
                },
                bottomBar = {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        if (!isSendEnabled) {
                            AiChatUpgradeBanner(onClick = onUpgradeClick)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .imePadding(),
                        ) {
                            OutlinedTextField(
                                value = state.input,
                                onValueChange = { intent.onInputChange(it) },
                                enabled = isSendEnabled,
                                placeholder = {
                                    Text(text = stringResource(R.string.ai_chat_input_label))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        isInputFocused = focusState.isFocused
                                    },
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (state.input.isBlank()) {
                                val buttonColor = if (isSendEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }

                                val contentColor = if (isSendEnabled) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                FilledIconButton(
                                    onClick = {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO,
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPermission) {
                                            val speechIntent = Intent(
                                                RecognizerIntent.ACTION_RECOGNIZE_SPEECH,
                                            ).apply {
                                                putExtra(
                                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                                )
                                                putExtra(
                                                    RecognizerIntent.EXTRA_LANGUAGE,
                                                    Locale.getDefault().toLanguageTag(),
                                                )
                                            }
                                            runCatching {
                                                speechRecognizerLauncher.launch(speechIntent)
                                            }.onFailure { e ->
                                                Timber.e(e, "Failed to launch speech intent")
                                            }
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    // Also gated on !state.isLoading (sdd-017, B-06): a button
                                    // left enabled mid-response invites a second, overlapping
                                    // request before the first one even finishes.
                                    enabled = isSendEnabled && !state.isLoading,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = buttonColor,
                                        contentColor = contentColor,
                                    ),
                                    modifier = Modifier.testTag("ai_chat_voice_button"),
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = stringResource(
                                            R.string.ai_chat_voice_content_description,
                                        ),
                                    )
                                }
                            } else {
                                FilledIconButton(
                                    onClick = { intent.onSendMessage() },
                                    // sdd-017, B-06: disabled while a response is in flight.
                                    enabled = isSendEnabled && !state.isLoading,
                                    modifier = Modifier.testTag("ai_chat_send_button"),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(
                                            CoreUiR.string.content_description_send_message,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { paddingValues ->
                val listState = rememberLazyListState()

                LaunchedEffect(state.messages.size, state.isLoading) {
                    val loadingOffset = if (state.isLoading) 1 else 0
                    val itemCount = state.messages.size + LEADING_AND_TRAILING_SPACERS + loadingOffset
                    if (itemCount > 0) {
                        listState.animateScrollToItem(itemCount - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        item {
                            if (state.messages.isEmpty()) {
                                AnimatedColorText(
                                    text = stringResource(R.string.ai_chat_greeting),
                                    fontSize = 32.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        items(state.messages) { message ->
                            ChatBubble(message = message)
                        }

                        if (state.isLoading) {
                            item { TypingBubble() }
                        }

                        item { Spacer(modifier = Modifier.height(4.dp)) }
                    }
                }
            }
        }
    }
}
