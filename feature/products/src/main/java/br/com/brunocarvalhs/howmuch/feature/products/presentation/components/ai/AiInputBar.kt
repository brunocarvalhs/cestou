package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

private const val MAX_LINES = 4
private val INPUT_BAR_HEIGHT = 48.dp
private val ICON_SPACING = 12.dp
private val BUTTON_SPACING = 8.dp

@Composable
fun AiInputBar(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    value: String,
    expanded: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAddClick: () -> Unit,
    onFocused: () -> Unit,
    onNotFocused: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(INPUT_BAR_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = stringResource(CoreR.string.content_description_ai_icon),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(ICON_SPACING))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    focusRequester?.let {
                        Modifier.focusRequester(it)
                    } ?: Modifier
                )
                .onFocusChanged {
                    if (it.isFocused) {
                        onFocused()
                    } else {
                        onNotFocused()
                    }
                },
            singleLine = false,
            maxLines = MAX_LINES,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(R.string.ai_input_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    innerTextField()
                }
            }
        )
        Spacer(Modifier.width(BUTTON_SPACING))
        FilledIconButton(
            onClick = {
                when {
                    expanded -> onSendClick()
                    else -> onAddClick()
                }
            }
        ) {
            AnimatedContent(
                targetState = expanded,
                label = "inputIcon"
            ) { send ->
                Icon(
                    imageVector =
                    if (send) Icons.AutoMirrored.Rounded.Send
                    else Icons.Rounded.Add,
                    contentDescription = if (send) {
                        stringResource(CoreR.string.content_description_send_message)
                    } else {
                        stringResource(CoreR.string.content_description_add_manually)
                    }
                )
            }
        }
    }
}

@Preview
@Composable
internal fun AiInputBarPreview() {
    AiInputBar(
        value = "",
        expanded = false,
        onValueChange = {},
        onSendClick = {},
        onAddClick = {},
        onFocused = {},
        onNotFocused = {}
    )
}

@Preview
@Composable
internal fun AiInputBarPreviewExpanded() {
    AiInputBar(
        value = "",
        expanded = true,
        onValueChange = {},
        onSendClick = {},
        onAddClick = {},
        onFocused = {},
        onNotFocused = {}
    )
}
