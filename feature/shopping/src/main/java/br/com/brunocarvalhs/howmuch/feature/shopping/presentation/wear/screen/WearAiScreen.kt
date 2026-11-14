package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import br.com.brunocarvalhs.howmuch.core.common.wearable.AiFailure
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouAiLoader
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
internal const val TAG_AI_NEW = "wear_ai_new"
internal const val TAG_AI_SUGGESTION_PREFIX = "wear_ai_suggestion_"

private val FieldRadius = 50.dp
private val LoaderSize = 32.dp
private val ThinkingLoaderSize = 64.dp
private val SuggestionsHeight = 60.dp
private val SuggestionWidth = 128.dp
private val FieldHeight = 44.dp
private val SendSize = 32.dp

/**
 * Assistente no relógio, em duas telas. Na inicial: o ícone de IA, a saudação, as perguntas
 * prontas e o campo embaixo. Ao perguntar, a tela vira só a resposta: o ícone animado fica no
 * centro enquanto pensa, e a resposta ocupa a tela, rolável. "Nova pergunta" volta à inicial.
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
    var prompt by rememberSaveable { mutableStateOf("") }
    val assistantTitle = stringResource(R.string.shopping_wear_ai_title)

    fun send() {
        val text = prompt.trim()
        if (text.isEmpty()) return
        onIntent(WearAiIntent.Send(prompt))
        prompt = ""
    }

    ScreenScaffold { contentPadding ->
        AnimatedContent(
            targetState = state is WearAiUiState.Idle,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.94f)).togetherWith(fadeOut())
            }
        ) { idle ->
            if (idle) {
                HomeView(
                    title = assistantTitle,
                    prompt = prompt,
                    onPromptChange = { if (it.length <= WEAR_AI_MAX_PROMPT_LENGTH) prompt = it },
                    onSend = ::send,
                    onIntent = onIntent
                )
            } else {
                ResponseView(state = state, title = assistantTitle, onIntent = onIntent)
            }
        }
    }
}

/** Tela inicial: ícone, saudação, perguntas prontas em carrossel e o campo fixo no fim. */
@Composable
private fun HomeView(
    title: String,
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onIntent: (WearAiIntent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CestouAiLoader(
            size = LoaderSize,
            modifier = Modifier.semantics { contentDescription = title }
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.shopping_wear_ai_greeting),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        SuggestionRow(onIntent = onIntent)
        PromptField(
            value = prompt,
            onValueChange = onPromptChange,
            enabled = true,
            sendEnabled = prompt.isNotBlank(),
            onSend = onSend,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

/**
 * Tela de resposta: ocupa a tela toda. Resposta e falha ficam no topo e rolam, com "Nova
 * pergunta" no fim da rolagem. Pensando e confirmação ficam no centro, sem rolagem.
 */
@Composable
private fun ResponseView(
    state: WearAiUiState,
    title: String,
    onIntent: (WearAiIntent) -> Unit
) {
    when (state) {
        is WearAiUiState.Answer, is WearAiUiState.Failed -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state is WearAiUiState.Answer) {
                Text(
                    text = state.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag(TAG_AI_ANSWER)
                )
            } else if (state is WearAiUiState.Failed) {
                Text(
                    text = stringResource(failureMessage(state.reason)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .testTag(TAG_AI_FAILED)
                )
            }
            Button(
                onClick = { onIntent(WearAiIntent.Reset) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
                    .testTag(TAG_AI_NEW),
                colors = wearCardColors()
            ) {
                Text(stringResource(R.string.shopping_wear_ai_new))
            }
        }
        else -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                WearAiUiState.Waiting -> ThinkingBlock(title)
                is WearAiUiState.Confirming -> ConfirmBlock(state, onIntent)
                else -> Unit
            }
        }
    }
}

/** Ícone animado de IA com o "pensando" embaixo, no centro da tela. */
@Composable
private fun ThinkingBlock(title: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CestouAiLoader(
            size = ThinkingLoaderSize,
            modifier = Modifier.semantics { contentDescription = title }
        )
        Text(
            text = stringResource(R.string.shopping_wear_ai_thinking),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TAG_AI_WAITING)
        )
    }
}

/** Ação de escrita pedida pelo agente: resumo e as duas escolhas, no centro da tela. */
@Composable
private fun ConfirmBlock(state: WearAiUiState.Confirming, onIntent: (WearAiIntent) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.shopping_wear_ai_confirm_title),
            textAlign = TextAlign.Center
        )
        Text(
            text = state.summary,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = { onIntent(WearAiIntent.Decide(approved = true)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TAG_AI_CONFIRM),
            colors = wearCardColors()
        ) {
            Text(stringResource(R.string.shopping_wear_ai_confirm))
        }
        OutlinedButton(
            onClick = { onIntent(WearAiIntent.Decide(approved = false)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TAG_AI_REJECT)
        ) {
            Text(stringResource(R.string.shopping_wear_ai_reject))
        }
    }
}

/** Perguntas prontas, todas de consulta: nenhuma executa escrita. */
private val SuggestionLabels = listOf(
    R.string.shopping_wear_ai_suggestion_spent,
    R.string.shopping_wear_ai_suggestion_lists,
    R.string.shopping_wear_ai_suggestion_budget
)

/** Perguntas prontas em carrossel horizontal: deslize para o lado para ver as outras. */
@Composable
private fun SuggestionRow(onIntent: (WearAiIntent) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(SuggestionsHeight),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(SuggestionLabels) { index, label ->
            val question = stringResource(label)
            Button(
                onClick = { onIntent(WearAiIntent.Send(question)) },
                modifier = Modifier
                    .width(SuggestionWidth)
                    .testTag("$TAG_AI_SUGGESTION_PREFIX$index"),
                colors = wearCardColors()
            ) {
                Text(text = question, maxLines = 3, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
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

/**
 * Barra de pergunta fixa no fim da tela inicial: uma pílula com o campo e o botão de enviar
 * dentro dela. A dica some quando há texto.
 */
@Composable
private fun PromptField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    sendEnabled: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hint = stringResource(R.string.shopping_wear_ai_hint)
    Row(
        modifier = modifier
            .height(FieldHeight)
            .clip(RoundedCornerShape(FieldRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .testTag(TAG_AI_FIELD),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(text = hint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    inner()
                }
            }
        )
        IconButton(
            onClick = onSend,
            enabled = sendEnabled,
            modifier = Modifier
                .size(SendSize)
                .testTag(TAG_AI_SEND)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.shopping_wear_ai_send),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
