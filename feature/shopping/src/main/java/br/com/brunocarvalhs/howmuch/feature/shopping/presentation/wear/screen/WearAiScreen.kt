package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import br.com.brunocarvalhs.howmuch.core.common.wearable.AiFailure
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearAiIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearAiUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WEAR_AI_MAX_PROMPT_LENGTH
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearAiViewModel

/** Tags para os testes de UI (AGENTS §5: sem asserts de texto). */
internal const val TAG_AI_FIELD = "wear_ai_field"
internal const val TAG_AI_SEND = "wear_ai_send"
internal const val TAG_AI_WAITING = "wear_ai_waiting"
internal const val TAG_AI_CONFIRM = "wear_ai_confirm"
internal const val TAG_AI_REJECT = "wear_ai_reject"
internal const val TAG_AI_ANSWER = "wear_ai_answer"
internal const val TAG_AI_FAILED = "wear_ai_failed"
internal const val TAG_AI_SUGGESTION_PREFIX = "wear_ai_suggestion_"

private val FieldRadius = 50.dp

/**
 * Entrada rápida de IA: um prompt curto, ditado ou digitado no teclado do relógio.
 * A resposta vem do celular; ações de escrita só rodam depois da confirmação na tela.
 */
@Composable
internal fun WearAiScreen(viewModel: WearAiViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WearAiContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
private fun WearAiContent(
    state: WearAiUiState,
    onIntent: (WearAiIntent) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    var prompt by rememberSaveable { mutableStateOf("") }
    val busy = state is WearAiUiState.Waiting || state is WearAiUiState.Confirming

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) {
                    Text(stringResource(R.string.shopping_wear_ai_title))
                }
            }

            if (!busy) {
                suggestionItems(onIntent = onIntent, spec = spec)
            }

            item {
                PromptField(
                    value = prompt,
                    onValueChange = { if (it.length <= WEAR_AI_MAX_PROMPT_LENGTH) prompt = it },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                )
            }

            item {
                Button(
                    onClick = {
                        onIntent(WearAiIntent.Send(prompt))
                        prompt = ""
                    },
                    enabled = prompt.isNotBlank() && !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .testTag(TAG_AI_SEND),
                    colors = wearCardColors(),
                    transformation = SurfaceTransformation(spec)
                ) {
                    Text(stringResource(R.string.shopping_wear_ai_send))
                }
            }

            stateItems(state = state, onIntent = onIntent, spec = spec)
        }
    }
}

/**
 * Perguntas prontas, todas de consulta: nenhuma executa escrita.
 */
private val SuggestionLabels = listOf(
    R.string.shopping_wear_ai_suggestion_spent,
    R.string.shopping_wear_ai_suggestion_lists,
    R.string.shopping_wear_ai_suggestion_budget
)

private fun TransformingLazyColumnScope.suggestionItems(
    onIntent: (WearAiIntent) -> Unit,
    spec: TransformationSpec
) {
    SuggestionLabels.forEachIndexed { index, label ->
        item {
            val question = stringResource(label)
            Button(
                onClick = { onIntent(WearAiIntent.Send(question)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .transformedHeight(this, spec)
                    .testTag("$TAG_AI_SUGGESTION_PREFIX$index"),
                colors = wearCardColors(),
                transformation = SurfaceTransformation(spec)
            ) {
                Text(text = question, maxLines = 2)
            }
        }
    }
}

/** A área de resposta: uma só por vez, conforme o estado. */
private fun TransformingLazyColumnScope.stateItems(
    state: WearAiUiState,
    onIntent: (WearAiIntent) -> Unit,
    spec: TransformationSpec
) {
    when (state) {
        WearAiUiState.Idle -> Unit
        WearAiUiState.Waiting -> item {
            Text(
                text = stringResource(R.string.shopping_wear_ai_thinking),
                modifier = Modifier.testTag(TAG_AI_WAITING)
            )
        }
        is WearAiUiState.Confirming -> {
            item {
                Text(text = stringResource(R.string.shopping_wear_ai_confirm_title))
            }
            item {
                Text(text = state.summary, style = MaterialTheme.typography.labelSmall)
            }
            item {
                Button(
                    onClick = { onIntent(WearAiIntent.Decide(approved = true)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .testTag(TAG_AI_CONFIRM),
                    colors = wearCardColors(),
                    transformation = SurfaceTransformation(spec)
                ) {
                    Text(stringResource(R.string.shopping_wear_ai_confirm))
                }
            }
            item {
                OutlinedButton(
                    onClick = { onIntent(WearAiIntent.Decide(approved = false)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                        .testTag(TAG_AI_REJECT),
                    transformation = SurfaceTransformation(spec)
                ) {
                    Text(stringResource(R.string.shopping_wear_ai_reject))
                }
            }
        }
        is WearAiUiState.Answer -> item {
            Text(
                text = state.text,
                modifier = Modifier.testTag(TAG_AI_ANSWER)
            )
        }
        is WearAiUiState.Failed -> item {
            Text(
                text = stringResource(failureMessage(state.reason)),
                modifier = Modifier.testTag(TAG_AI_FAILED)
            )
        }
    }
}

/** Mensagem para cada motivo de falha: o do celular (AiFailure) ou o do próprio relógio. */
private fun failureMessage(reason: String): Int = when (reason) {
    AiFailure.NO_PROVIDER -> R.string.shopping_wear_ai_no_provider
    WearAiUiState.PHONE_UNAVAILABLE -> R.string.shopping_wear_ai_phone_unavailable
    WearAiUiState.TIMEOUT -> R.string.shopping_wear_ai_timeout
    else -> R.string.shopping_wear_ai_failed
}

/** Campo do prompt, com dica quando vazio. Mesmo visual dos campos de busca do relógio. */
@Composable
private fun PromptField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val hint = stringResource(R.string.shopping_wear_ai_hint)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .clip(RoundedCornerShape(FieldRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(TAG_AI_FIELD),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(text = hint)
                }
                inner()
            }
        }
    )
}
