package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouBrightGreen
import br.com.brunocarvalhs.howmuch.feature.products.R

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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(4.dp, RoundedCornerShape(28.dp))
            .background(Color.White, RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
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
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = "Mensagem para o Cestou...",
                            color = Color.Gray.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )

            IconButton(onClick = { /* Mic action */ }) {
                Icon(
                    imageVector = Icons.Default.MicNone,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            IconButton(
                onClick = onSendClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CestouBrightGreen)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}
