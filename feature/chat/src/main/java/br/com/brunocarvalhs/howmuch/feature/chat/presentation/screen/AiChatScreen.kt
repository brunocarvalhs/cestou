package br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.theme.CestouBrightGreen
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.lazy.grid.items as gridItems

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private fun Instant.toTimeLabel(): String = timeFormatter.format(this)

private const val EMOJI_ICON = "😊"
private const val BUBBLE_MAX_WIDTH_DP = 280
private const val BUBBLE_CORNER_RADIUS_DP = 18
private const val BUBBLE_TAIL_RADIUS_DP = 4
private const val AVATAR_SIZE_DP = 40
private const val EMOJI_PICKER_HEIGHT_DP = 260
private const val EMOJI_PICKER_COLUMNS = 8
// The LazyColumn always wraps state.messages with one leading and one trailing Spacer item.
private const val LEADING_AND_TRAILING_SPACERS = 2

private val QUICK_EMOJIS = listOf(
    "😀", "😁", "😂", "🤣", "😊", "😍", "😘", "😜",
    "🤔", "😴", "😢", "😭", "😡", "👍", "👎", "👏",
    "🙏", "💪", "🎉", "❤️", "🔥", "✨", "⭐", "💡",
    "✅", "❌", "🛒", "🥦", "🍎", "🥕", "🍞", "🧃",
    "🍕", "🍔", "🍩", "☕", "💰", "📅", "⏰", "😅"
)

private data class ChatSuggestion(val id: String, @StringRes val textRes: Int)

// Shown on the empty-state so the first screen isn't a blank input box — picking one of
// these prefills and sends in one tap, no typing required.
private val EMPTY_STATE_SUGGESTIONS = listOf(
    ChatSuggestion("recipe", R.string.ai_chat_suggestion_recipe),
    ChatSuggestion("bbq", R.string.ai_chat_suggestion_bbq),
    ChatSuggestion("repeat_purchase", R.string.ai_chat_suggestion_repeat_purchase),
    ChatSuggestion("budget_tip", R.string.ai_chat_suggestion_budget_tip)
)

// Shown under the latest assistant reply to invite a second message.
private val FOLLOW_UP_SUGGESTIONS = listOf(
    ChatSuggestion("add_all", R.string.ai_chat_followup_add_all),
    ChatSuggestion("more_ideas", R.string.ai_chat_followup_more_ideas)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    state: AiChatUiState,
    intent: AiChatIntent,
    isSendEnabled: Boolean = true,
    onUpgradeClick: () -> Unit = {}
) {
    var showEmojiPicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(AVATAR_SIZE_DP.dp)
                                .clip(CircleShape)
                                .background(CestouBrightGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.ai_chat_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = intent.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                br.com.brunocarvalhs.howmuch.core.ui.R.string.content_description_back
                            )
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
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                if (!isSendEnabled) {
                    AiChatUpgradeBanner(onClick = onUpgradeClick)
                }
                state.error?.let { error ->
                    AiChatErrorBanner(message = error.asString())
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
                        placeholder = { Text(text = stringResource(R.string.ai_chat_input_label)) },
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    if (showEmojiPicker) {
                                        showEmojiPicker = false
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    } else {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        showEmojiPicker = true
                                    }
                                }
                            ) {
                                if (showEmojiPicker) {
                                    Icon(
                                        imageVector = Icons.Default.Keyboard,
                                        contentDescription = stringResource(
                                            R.string.ai_chat_keyboard_content_description
                                        )
                                    )
                                } else {
                                    Text(text = EMOJI_ICON, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_chat_input")
                            .focusRequester(focusRequester)
                            .onFocusEvent { if (it.isFocused) showEmojiPicker = false },
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { intent.onSendMessage() },
                        enabled = isSendEnabled,
                        modifier = Modifier.testTag("ai_chat_send_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(
                                br.com.brunocarvalhs.howmuch.core.ui.R.string.content_description_send_message
                            )
                        )
                    }
                }

                if (showEmojiPicker) {
                    EmojiPickerPanel(
                        onEmojiSelected = { emoji -> intent.onInputChange(state.input + emoji) },
                        modifier = Modifier.height(EMOJI_PICKER_HEIGHT_DP.dp)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        val listState = rememberLazyListState()

        // listState.layoutInfo.totalItemsCount lags one layout pass behind the state change
        // that triggers this effect, so it must not be used to compute the scroll target —
        // derive the count from the same state driving the LazyColumn content below instead.
        LaunchedEffect(state.messages.size, state.isLoading) {
            val itemCount = state.messages.size + LEADING_AND_TRAILING_SPACERS + if (state.isLoading) 1 else 0
            if (itemCount > 0) {
                listState.animateScrollToItem(itemCount - 1)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Extra items beyond LEADING_AND_TRAILING_SPACERS (empty state, follow-up chips)
            // aren't accounted for in the scroll-target formula above; animateScrollToItem
            // clamps to the last item when the index overshoots, so this is harmless.
            if (state.messages.isEmpty() && !state.isLoading) {
                item {
                    AiChatEmptyState(
                        onSuggestionSelected = intent.onSuggestionSelected,
                        enabled = isSendEnabled
                    )
                }
            }

            items(state.messages) { message ->
                ChatBubble(message = message)
            }

            if (state.isLoading) {
                item {
                    TypingBubble()
                }
            }

            if (!state.isLoading && state.messages.lastOrNull()?.sender == ChatMessage.Sender.ASSISTANT) {
                item {
                    AiChatFollowUpSuggestions(
                        onSuggestionSelected = intent.onSuggestionSelected,
                        enabled = isSendEnabled
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }
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
private fun EmojiPickerPanel(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(EMOJI_PICKER_COLUMNS),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentPadding = PaddingValues(8.dp)
    ) {
        gridItems(QUICK_EMOJIS) { emoji ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onEmojiSelected(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
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

@Composable
private fun AiChatEmptyState(
    onSuggestionSelected: (id: String, text: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(AVATAR_SIZE_DP.dp)
                .clip(CircleShape)
                .background(CestouBrightGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.ai_chat_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.ai_chat_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EMPTY_STATE_SUGGESTIONS.forEach { suggestion ->
                val text = stringResource(suggestion.textRes)
                SuggestionChip(
                    onClick = { onSuggestionSelected(suggestion.id, text) },
                    label = { Text(text) },
                    enabled = enabled,
                    modifier = Modifier.testTag("ai_chat_suggestion_${suggestion.id}")
                )
            }
        }
    }
}

@Composable
private fun AiChatFollowUpSuggestions(
    onSuggestionSelected: (id: String, text: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FOLLOW_UP_SUGGESTIONS.forEach { suggestion ->
            val text = stringResource(suggestion.textRes)
            SuggestionChip(
                onClick = { onSuggestionSelected(suggestion.id, text) },
                label = { Text(text) },
                enabled = enabled,
                modifier = Modifier.testTag("ai_chat_followup_${suggestion.id}")
            )
        }
    }
}

// Matches CestouLockedBanner's errorContainer treatment (core:ui) — this is a hit-a-limit
// alert, not a positive/brand-colored message, so it must not read as primaryContainer green.
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

@Composable
private fun AiChatErrorBanner(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = UPGRADE_BANNER_BG_ALPHA)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = UPGRADE_BANNER_TEXT_ALPHA)
            )
        }
    }
}
