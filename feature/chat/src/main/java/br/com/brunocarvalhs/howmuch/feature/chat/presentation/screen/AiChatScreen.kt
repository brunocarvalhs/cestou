package br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.component.AnimatedColorText
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private fun Instant.toTimeLabel(): String = timeFormatter.format(this)

private const val BUBBLE_MAX_WIDTH_DP = 280
private const val BUBBLE_CORNER_RADIUS_DP = 18
private const val BUBBLE_TAIL_RADIUS_DP = 4
private const val LEADING_AND_TRAILING_SPACERS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    state: AiChatUiState,
    intent: AiChatIntent,
    isSendEnabled: Boolean = true,
    onUpgradeClick: () -> Unit = {}
) {
    var isInputFocused by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    val isChatActive by rememberUpdatedState(
        newValue = state.messages.isNotEmpty() || isInputFocused
    )

    val animatedHeight by animateFloatAsState(
        targetValue = if (isChatActive) 1f else 0.4f,
        label = "bottomSheetHeight"
    )

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { newState ->
            !(isChatActive && newState != androidx.compose.material3.SheetValue.Expanded)
        }
    )

    LaunchedEffect(isChatActive) {
        if (isChatActive && sheetState.currentValue != androidx.compose.material3.SheetValue.Expanded) {
            sheetState.expand()
        }
    }

    androidx.activity.compose.BackHandler(enabled = isChatActive) {
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
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedHeight)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        title = { },
                        navigationIcon = {
                            IconButton(onClick = intent.onToggleHistory) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = stringResource(R.string.ai_chat_history_content_description)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = intent.onSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.ai_chat_settings_content_description)
                                )
                            }
                            IconButton(onClick = intent.onBack) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.ai_chat_close_content_description)
                                )
                            }
                        }
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
                                .imePadding()
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
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (state.input.isBlank()) {
                                FilledIconButton(
                                    onClick = { intent.onStartVoiceInput() },
                                    enabled = isSendEnabled
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = stringResource(
                                            R.string.ai_chat_voice_content_description
                                        )
                                    )
                                }
                            } else {
                                FilledIconButton(
                                    onClick = { intent.onSendMessage() },
                                    enabled = isSendEnabled
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(
                                            br.com.brunocarvalhs.howmuch.core.ui.R.string.content_description_send_message
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { paddingValues ->
                val listState = rememberLazyListState()

                LaunchedEffect(state.messages.size, state.isLoading) {
                    val itemCount = state.messages.size +
                            LEADING_AND_TRAILING_SPACERS +
                            if (state.isLoading) 1 else 0
                    if (itemCount > 0) {
                        listState.animateScrollToItem(itemCount - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        item {
                            if (state.messages.isEmpty()) {
                                AnimatedColorText(
                                    text = stringResource(R.string.ai_chat_greeting),
                                    fontSize = 32.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == ChatMessage.Sender.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = RoundedCornerShape(
        topStart = BUBBLE_CORNER_RADIUS_DP.dp,
        topEnd = BUBBLE_CORNER_RADIUS_DP.dp,
        bottomStart = if (isUser) BUBBLE_CORNER_RADIUS_DP.dp else BUBBLE_TAIL_RADIUS_DP.dp,
        bottomEnd = if (isUser) BUBBLE_TAIL_RADIUS_DP.dp else BUBBLE_CORNER_RADIUS_DP.dp
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            modifier = Modifier.widthIn(max = BUBBLE_MAX_WIDTH_DP.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Markdown(
                    content = message.text,
                    colors = markdownColor(
                        text = textColor,
                        codeBackground = textColor.copy(alpha = 0.12f),
                        inlineCodeBackground = textColor.copy(alpha = 0.12f),
                        dividerColor = textColor.copy(alpha = 0.3f)
                    ),
                    typography = markdownTypography(
                        text = MaterialTheme.typography.bodyMedium,
                        paragraph = MaterialTheme.typography.bodyMedium,
                        ordered = MaterialTheme.typography.bodyMedium,
                        bullet = MaterialTheme.typography.bodyMedium,
                        list = MaterialTheme.typography.bodyMedium
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.createdAt.toTimeLabel(),
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = BUBBLE_CORNER_RADIUS_DP.dp,
                topEnd = BUBBLE_CORNER_RADIUS_DP.dp,
                bottomStart = BUBBLE_TAIL_RADIUS_DP.dp,
                bottomEnd = BUBBLE_CORNER_RADIUS_DP.dp
            )
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
    }
}

private const val UPGRADE_BANNER_BG_ALPHA = 0.7f
private const val UPGRADE_BANNER_TEXT_ALPHA = 0.8f

@Composable
private fun AiChatUpgradeBanner(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = UPGRADE_BANNER_BG_ALPHA)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.ai_chat_upgrade_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = UPGRADE_BANNER_TEXT_ALPHA),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

private val previewMessages = listOf(
    ChatMessage(id = 1, text = "Quanto vou gastar nessa lista?", sender = ChatMessage.Sender.USER),
    ChatMessage(
        id = 2,
        text = "Com base nos itens adicionados, o total estimado é de R$ 87,40.",
        sender = ChatMessage.Sender.ASSISTANT
    ),
    ChatMessage(
        id = 3, text = "Consegue sugerir algo mais barato?", sender = ChatMessage.Sender.USER
    )
)

@PreviewCestouScreens
@Composable
private fun AiChatScreenEmptyPreview() {
    CestouTheme {
        AiChatScreen(
            state = AiChatUiState(), intent = AiChatIntent()
        )
    }
}

@PreviewCestouScreens
@Composable
private fun AiChatScreenConversationPreview() {
    CestouTheme {
        AiChatScreen(
            state = AiChatUiState(
                messages = previewMessages, input = "Consegue sugerir algo mais barato?"
            ), intent = AiChatIntent()
        )
    }
}

@PreviewCestouScreens
@Composable
private fun AiChatScreenLoadingPreview() {
    CestouTheme {
        AiChatScreen(
            state = AiChatUiState(
                messages = previewMessages, isLoading = true
            ), intent = AiChatIntent()
        )
    }
}

@PreviewCestouScreens
@Composable
private fun AiChatScreenTrialUsedPreview() {
    CestouTheme {
        AiChatScreen(
            state = AiChatUiState(messages = previewMessages),
            intent = AiChatIntent(),
            isSendEnabled = false
        )
    }
}
