package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.ai

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.Suggestions
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.AiDockState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val COLLAPSED_HEIGHT = 82
private const val EXPANDED_HEIGHT = 180
private const val CHAT_SHAPE = 32
private const val DEFAULT_SHAPE = 24
private const val TONAL_ELEVATION = 8
private const val SHADOW_ELEVATION = 16
private const val FOCUS_DELAY_MS = 100L
private const val PADDING_HORIZONTAL = 20
private const val SHAPE_ANIMATION_DURATION_MS = 300

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun CartAssistantDock(
    modifier: Modifier = Modifier,
    state: AiDockState,
    suggestions: List<String> = emptyList(),
    isSuggestionsLoading: Boolean = false,
    messages: StableList<ChatMessage> = StableList(),
    loading: Boolean = false,
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAddClick: () -> Unit,
    onToggleClick: () -> Unit,
    onSuggestionClick: (String) -> Unit = {},
    onFocused: () -> Unit = {},
    onNotFocused: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current

    val height by animateDpAsState(
        targetValue = when (state) {
            AiDockState.COLLAPSED -> COLLAPSED_HEIGHT.dp
            AiDockState.EXPANDED -> EXPANDED_HEIGHT.dp
            AiDockState.CHAT -> configuration.screenHeightDp.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dockHeight"
    )

    val shape by animateDpAsState(
        targetValue = if (state == AiDockState.CHAT) CHAT_SHAPE.dp else DEFAULT_SHAPE.dp,
        animationSpec = tween(SHAPE_ANIMATION_DURATION_MS),
        label = "dockShape"
    )

    LaunchedEffect(state) {
        if (state == AiDockState.EXPANDED) {
            delay(FOCUS_DELAY_MS.milliseconds)
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(shape),
        tonalElevation = TONAL_ELEVATION.dp,
        shadowElevation = SHADOW_ELEVATION.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = state == AiDockState.CHAT,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Unspecified,
                        navigationIconContentColor = Color.Unspecified,
                        titleContentColor = Color.Unspecified,
                        actionIconContentColor = Color.Unspecified
                    ),
                    title = { Text(stringResource(R.string.ai_assistant_title)) },
                    navigationIcon = {
                        IconButton(onClick = onToggleClick) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    modifier = Modifier.fillMaxSize(),
                    targetState = state,
                    transitionSpec = {
                        slideInVertically { it / 2 } + fadeIn() togetherWith
                            slideOutVertically { it / 2 } + fadeOut()
                    },
                    label = "aiContent"
                ) { currentState ->
                    when (currentState) {
                        AiDockState.CHAT -> ChatScreen(
                            listState = listState,
                            messages = messages.items,
                            loading = loading
                        )

                        AiDockState.EXPANDED -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        horizontal = PADDING_HORIZONTAL.dp,
                                    )
                            ) {
                                Suggestions(
                                    items = suggestions,
                                    isLoading = isSuggestionsLoading,
                                    onItemClick = onSuggestionClick
                                )
                            }
                        }

                        AiDockState.COLLAPSED -> {
                            Spacer(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            AiInputBar(
                modifier = Modifier.padding(PADDING_HORIZONTAL.dp),
                focusRequester = focusRequester,
                value = value,
                expanded = state != AiDockState.COLLAPSED,
                onValueChange = onValueChange,
                onSendClick = onSendClick,
                onAddClick = onAddClick,
                onFocused = onFocused,
                onNotFocused = onNotFocused
            )
        }
    }
}

@Preview
@Composable
internal fun CartAssistantDockPreviewStateChat() {
    CartAssistantDock(
        state = AiDockState.CHAT,
        value = "",
        onValueChange = {},
        onSendClick = {},
        onAddClick = {},
        onToggleClick = {}
    )
}

@Preview
@Composable
internal fun CartAssistantDockPreviewStateExpanded() {
    CartAssistantDock(
        state = AiDockState.EXPANDED,
        value = "",
        onValueChange = {},
        onSendClick = {},
        onAddClick = {},
        onToggleClick = {}
    )
}

@Preview
@Composable
internal fun CartAssistantDockPreviewStateCollapsed() {
    CartAssistantDock(
        state = AiDockState.COLLAPSED,
        value = "",
        onValueChange = {},
        onSendClick = {},
        onAddClick = {},
        onToggleClick = {}
    )
}
