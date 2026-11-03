# Task: Padronizar o fundo do drag handle dos modais, corrigir o voltar travado no chat e não cancelar a resposta da IA ao fechar

- **Slug:** modais-cores-consistentes
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready
- **Versão-alvo:** 2.2.2 (patch, fix visual + fix de navegação)
- **Tipo de mudança:** fix
- **Impacto público (módulo):** additive (um Composable público novo em `core:ui`, ver §4)

## 1. Contexto e Motivação
- Pedido do usuário: "os modais estão com cores diferentes", esclarecido como o fundo do drag handle: "o Drag handle de fundo deve ser o mesmo do background". Decisões do usuário: Q1 validação manual; Q2 dialogs ignorados (só os sheets têm o problema).
- **Item 1: cor.** O handle é desenhado sobre o container do `ModalBottomSheet`. O tema não define `surfaceContainer*` (`core/theme/.../Theme.kt:13-57`), então o padrão é `surfaceContainerLow`, roxo-acinzentado do baseline M3. Quem passa `containerColor = surface` fica branco. O `Scaffold` interno do Chat e do Products usa `background`. O resultado são três fundos de handle e uma tarja entre handle e corpo.
  - Sheets sem `containerColor` (roxo padrão): `NotificationSettingsScreen.kt:104`, `ThemeSettingsScreen.kt:84`, `DataSettingsScreen.kt:168`, `ShoppingGraph.kt:126,204`, `ProductsGraph.kt:25`, `CartGraph.kt:135,166,189,228`, `FeedbackSurveyScreen.kt:42`, `AiChatScreen.kt:162`, `core/ui/.../ShareOptionsBottomSheet.kt:38`.
  - Sheets com `containerColor = surface` (branco): `QrCodeBottomSheet.kt:66-69`, `ShoppingScreen.kt:181-185`, `WelcomeScreen.kt:159-162,230-235`.
  - Exceção intencional: Scanner com `Color.Black` e handle branco (`ShoppingGraph.kt:186-189`).
- **Item 2: bug do voltar no chat.** Com o chat ativo (há mensagens ou o campo está focado), o botão voltar nativo não fecha o modal e a tela fica travada; só o botão "fechar" funciona.
  - Hipótese (não reproduzida ainda): `isChatActive` (`AiChatScreen.kt:136-138`) faz o `confirmValueChange` rejeitar qualquer valor diferente de `Expanded` (`:145-150`), inclusive `Hidden`. O voltar e o toque no scrim do `ModalBottomSheet` chamam `sheetState.hide()` internamente. A transição é vetada, o `onDismissRequest = intent.onBack` (`:163`) nunca dispara e o sheet fica preso com o scrim.
  - O `BackHandler(enabled = isChatActive)` (`:158-160`) fica na composição externa (o `dialog<AiChat>` do `ChatGraph.kt:34`). O sheet é uma janela própria, e seu handler de back tem prioridade sobre o externo. Por isso o `BackHandler` não resolve.
  - `AiChatScreen.kt:162` e `ChatGraph.kt:34-62` (`onBack = { navigator.goBack() }`).
- **Item 3: resposta da IA não pode ser cancelada ao fechar (decisão do usuário, Q3):** "não deve nunca terminar, vale criar uma coroutine desvinculada da chamada que trabalha em segundo plano; senão, fechar o chat cancela a IA e impacta a espera do cliente".
  - Hoje `sendMessage` roda em `viewModelScope.launch` (`AiChatViewModel.kt:~215`). O `AiChatViewModel` pertence à entrada `AiChat` do back stack (`ChatGraph.kt:36`); ao fechar, o `viewModelScope` é cancelado e a chamada à IA morre no meio. O `catch` relança `CancellationException` (`:~250`), então nem a mensagem de erro aparece.
  - `persistHistory()` também usa `viewModelScope` (`:~268`), e a resposta tardia não seria salva. Já existe precedente para o mesmo problema: `AiTrialRepositoryImpl.kt:24-30` usa `NonCancellable` para a escrita do trial, citando que "o scope é cancelado no instante em que a tela fecha".
  - Não existe `CoroutineScope` de aplicação injetável: os scopes são criados ad hoc (`FirebaseAuthService.kt:36`, `PlayBillingSubscriptionRepository.kt:58`).
  - `AiSession` é `@Singleton` (`MainViewModel.kt:63`). Reabrir o chat chama `aiSession.clearHistory()` em `setShoppingContext`/`loadSession` (`AiChatViewModel.kt:~95-112`) e zera `fallbackUsedThisTurn` em novo envio, o que corromperia o turno ainda em andamento.
  - `persistHistory()` retorna sem salvar quando não há `sessionId` nem `shoppingId` (`:~270-278`), e só o histórico cria sessão (`AiHistoryViewModel.kt:61`). Num chat sem contexto, uma resposta tardia se perderia.

## 2. Escopo
- **Inclui:**
  - Wrapper `CestouModalBottomSheet` em `core:ui` com `containerColor = MaterialTheme.colorScheme.background` por padrão.
  - Migrar todos os `ModalBottomSheet` de §1 para o wrapper, removendo os `containerColor = surface` locais. O Scanner passa `Color.Black`.
  - Reproduzir o bug do voltar no chat e corrigir o veto do `Hidden` em `confirmValueChange`, para que voltar e scrim fechem o sheet mesmo com o chat ativo.
  - Remover o `BackHandler` externo do chat se, após a correção, continuar redundante.
  - Executar o turno da IA e a persistência da resposta num scope de aplicação (`SupervisorJob`), independente do `viewModelScope`, para que fechar o chat não cancele a chamada.
  - Um rastreador singleton de turnos em andamento (`ChatTurnTracker`), para que um chat reaberto saiba que há turno pendente (mostra `isLoading`, não limpa o `AiSession` e não envia em paralelo) e recarregue o histórico quando o turno terminar.
  - Salvar a resposta tardia mesmo sem `sessionId`/`shoppingId`, criando a sessão (ver Q4).
- **Exclui:**
  - Notificação ou aviso ao usuário quando a resposta chegar com o chat fechado (ver Q5).
  - Cancelamento explícito pelo usuário ("parar resposta"): fora do pedido.
  - Tema e tokens `surfaceContainer*`, `AlertDialog`/`CategoryPickerDialog` (decisão Q2), shapes, insets, indicador do handle e `:wear`.
  - O gesto de arrastar do chat continua desabilitado com o chat ativo (`sheetGesturesEnabled = !isChatActive`, `:165`).

## 3. Impacto Arquitetural
- Módulos: `:core:ui` (wrapper) e as features `settings`, `shopping`, `products`, `cart`, `feedback`, `chat`, `auth` (`presentation`/`navigation`). Sem dependência feature→feature (AGENTS.md §2).
- Novo arquivo: `core/ui/src/main/java/br/com/brunocarvalhs/howmuch/core/ui/components/CestouModalBottomSheet.kt`.
- Item 3, `:feature:chat` (`di/` e `presentation/viewmodel`; o rastreador no `domain/` como contrato e implementação em `data/` ou `presentation/helper`, a decidir pelo implementador conforme `android-architecture`):
  - `feature/chat/src/main/java/.../feature/chat/domain/service/ChatTurnTracker.kt` (interface)
  - implementação `@Singleton` correspondente
  - qualificador e provider do scope em `di/ChatModule.kt`
- DI: scope de aplicação provido por Hilt no `SingletonComponent` (`CoroutineScope(SupervisorJob() + Dispatchers.Default)`), injetado no `AiChatViewModel`; sem service locator (AGENTS.md §5). O Item 3 não adiciona dependência Gradle.

```
 core:ui  CestouModalBottomSheet ─► ModalBottomSheet(containerColor = colorScheme.background)
              ▲
 feature:* ───┘  (todas as chamadas migradas; Scanner passa Color.Black)

 Chat (isChatActive): voltar / scrim ─► sheetState.hide() ─► confirmValueChange(Hidden)=true
                      ─► onDismissRequest ─► intent.onBack ─► navigator.goBack()
```

## 4. Contratos e Interfaces
Assinatura (sem implementação):

```kotlin
// core:ui, public: consumido por 7 módulos de feature (não pode ser internal)
@Composable
public fun CestouModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = MaterialTheme.colorScheme.background,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    sheetGesturesEnabled: Boolean = true,
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    content: @Composable ColumnScope.() -> Unit,
)
```
- Os defaults espelham os do `ModalBottomSheet` do Material3; só `containerColor` muda.
- Item 2 não muda contratos: apenas a lambda `confirmValueChange` (interna a `AiChatScreen`).
- Item 3 (assinaturas, `internal` por padrão; sem uso fora de `:feature:chat`):

```kotlin
@Qualifier @Retention(AnnotationRetention.BINARY)
annotation class ChatBackgroundScope            // mesmo padrão de ChatDataStore (ChatModule.kt:21)

interface ChatTurnTracker {                  // public: ver nota abaixo
    val inFlight: StateFlow<Set<String>>        // chave = sessionId ?: shoppingId
    fun begin(key: String)
    fun end(key: String)
}

class AiChatViewModel @Inject constructor(
    /* ...atuais... */
    @ChatBackgroundScope private val backgroundScope: CoroutineScope,
    private val turnTracker: ChatTurnTracker,
)
```
- **Ajuste pós-implementação:** `ChatTurnTracker` ficou `public` (e não `internal`): o construtor público do `AiChatViewModel` o expõe e `:app` (`MainViewModel`) o usa para `cancelAll()` no logout, como `ChatHistoryRepository` já é público. A implementação `ChatTurnTrackerImpl` continua `internal`.
- O construtor do `AiChatViewModel` é público e ganha dois parâmetros: impacto `additive`; os testes que o instanciam (`AiChatViewModelTest.kt`, `AiChatViewModelReliabilityTest.kt`) são atualizados. O qualificador fica público apenas por necessidade do Hilt, como `ChatDataStore`.
- Sem `NavKey`, repositório, Firestore, remote-config ou evento de Analytics novos. Sem breaking change.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | Sheets usam roxo padrão ou `surface` | Fundo e handle em `background` | lista em §1 |
| 2 | Chat e Products: handle roxo, corpo `background` | Handle e corpo iguais, sem tarja | `AiChatScreen.kt:162,178`, `ProductScreen.kt:58` |
| 3 | Scanner `Color.Black` | Mantido | `ShoppingGraph.kt:186-189` |
| 4 | Chat ativo: `confirmValueChange` rejeita tudo que não seja `Expanded`, e voltar/scrim ficam travados | Aceita `Hidden` (dismiss por voltar/scrim/fechar); continua rejeitando `PartiallyExpanded` | `AiChatScreen.kt:145-150` |
| 5 | `onDismissRequest = intent.onBack` fecha via `navigator.goBack()` | Inalterado | `AiChatScreen.kt:163`, `ChatGraph.kt:49` |
| 6 | Sair da composição do dialog chama `onClose()` para o CSAT | Inalterado; o voltar agora também o dispara | `ChatGraph.kt:43-45` |
| 7 | O turno da IA roda em `viewModelScope` e morre ao fechar o chat | Roda no scope de aplicação e só termina por conclusão ou falha da chamada, nunca por fechamento da tela | `AiChatViewModel.kt:~215` |
| 8 | A persistência da resposta usa `viewModelScope` | Usa o scope de aplicação; a resposta e a mensagem de erro chegam ao histórico mesmo com o chat fechado | `AiChatViewModel.kt:~268` |
| 9 | `markFreeMessageUsed` e `trialMessageUsed` dependem de a coleta terminar | Executam ao fim do turno desacoplado | `AiChatViewModel.kt:~240-243`, `AiTrialRepositoryImpl.kt:24-30` |
| 10 | Reabrir o chat limpa o `AiSession` singleton e permite novo envio | Com turno em andamento: `setShoppingContext` abre um chat novo e espera os turnos terminarem antes de limpar o `AiSession` (`awaitIdleTurns`); `loadSession` de conversa com turno pendente mostra `isLoading`, bloqueia envio e recarrega o histórico ao terminar | `AiChatViewModel.kt:~95-112,~190` |
| 11 | Sem `sessionId`/`shoppingId` nada é persistido | Todo chat novo ganha um `sessionId` (`UUID.randomUUID()`, e não `createNewSession`, que deixaria o título "Nova conversa") antes do primeiro envio, mesmo vindo de uma lista; a persistência usa só `saveSession` (Q4) | `AiChatViewModel.kt:~270-278`, `ChatHistoryRepositoryImpl.kt:105` |

## 6. Critérios de Aceitação
- **Cor:**
  - Todos os sheets, exceto o Scanner, exibem handle e corpo em `colorScheme.background` (`#F9FAFB` no claro, `#030712` no escuro), sem tarja nem tom roxo.
  - Nenhum `ModalBottomSheet(` direto em `feature/**` e `core/ui/**`, exceto no wrapper.
  - Conteúdo em `surface`/`surfaceVariant` (cards, `ProductListItem.kt:84`, `ProductAnalysisConfirmation.kt:59`, campos) continua distinguível do fundo em light e dark.
  - O indicador do handle é visível sobre `background`.
- **Voltar no chat:**
  - Com mensagens no chat, o voltar nativo fecha o modal e volta à tela anterior; sem travar a tela nem deixar scrim.
  - Idem com o campo focado e teclado aberto (o primeiro voltar pode fechar o teclado, conforme o comportamento do sistema; o seguinte fecha o modal).
  - Toque no scrim e botão "fechar" continuam funcionando.
  - Voltar com o chat inativo (estado inicial a 40% de altura) continua funcionando.
  - Gesto de arrastar segue desabilitado com o chat ativo; o sheet não sai de `Expanded` por gesto.
  - Gesto de voltar preditivo (Android 13+) sem estado visual preso.
- **Resposta da IA desacoplada:**
  - Com uma resposta em andamento, fechar o chat (voltar, scrim ou "fechar") não cancela a chamada; a resposta é salva no histórico e aparece ao reabrir a mesma conversa.
  - Falha de rede ou da IA com o chat fechado grava a mensagem de erro no histórico (mesmos textos de `R.string.ai_chat_error_network`/`ai_chat_error_generic`) e libera o rastreador.
  - Reabrir durante o turno mostra o indicador de carregamento, não permite segundo envio e atualiza sozinho quando o turno termina; o `AiSession` não é limpo nesse intervalo.
  - Trocar de lista ou sessão durante um turno de outra conversa não mistura mensagens entre históricos.
  - Logout (`MainViewModel.kt:63`) continua limpando o `AiSession`; o plano deve definir se um turno pendente é descartado ao trocar de conta (Q6).
  - O scope de aplicação usa `SupervisorJob`: uma falha em um turno não derruba outros nem o processo; exceções são tratadas no próprio turno, sem engolir `CancellationException` do scope de aplicação.
  - Sem vazamento: o turno desacoplado não mantém `Activity`/`Context` de UI (o ViewModel só tem `@ApplicationContext`).
- Performance: o item 1 é leitura de CompositionLocal e uma lambda; o item 3 não cria thread nova, usa `Dispatchers.Default` e chamadas suspendíveis; a persistência continua em uma única escrita por mensagem, sem I/O duplicado.
- Feature toggles: não aplicável. Observabilidade: sem eventos novos; o evento de fim de chat já sai em `ChatGraph.kt:43-45` (garantir que não dispara duas vezes). A telemetria de resposta (`responseReceived`/`responseFailed`) deve continuar sendo emitida mesmo com o chat fechado, sem dados pessoais.
- Compatibilidade: minSdk 28; testar voltar com 3 botões e gestual; Wear e dados não afetados.
- Sem strings novas e sem `@Suppress`. `CHANGELOG.md` com duas entradas de fix (AGENTS.md §3 e §6.5).

## 7. Plano de Testes
- **Reprodução do bug (antes de corrigir):** Maestro com `testTag`, `pressBack` com chat ativo, confirmando o sheet preso. Se não reproduzir com a hipótese, parar e reavaliar a causa (ex.: `BackHandler` externo capturando o evento).
- **Unitários:**
  - Extrair a regra de `confirmValueChange` para uma função pura `internal` (ex.: `isSheetValueAllowed(isChatActive, target)`) e testar: chat ativo permite `Expanded` e `Hidden`, rejeita `PartiallyExpanded`; chat inativo permite todos.
  - Os testes existentes de `feature/chat/src/test/.../screen` e `.../viewmodel` seguem passando.
  - `AiChatViewModelTest`/`AiChatViewModelReliabilityTest` (coroutines test, scope de aplicação injetado como `TestScope`):
    - cancelar o `viewModelScope` (`onCleared`) durante um turno não cancela a coleta; a resposta é salva via `ChatHistoryRepository`;
    - falha `IOException` e erro genérico após o `onCleared` gravam a mensagem de erro e liberam o rastreador;
    - `markFreeMessageUsed` é chamado após resposta tardia; nada é chamado se a IA não respondeu;
    - segundo envio durante turno em andamento continua bloqueado; reabertura com turno pendente mostra `isLoading` e não chama `clearHistory`;
    - ao terminar o turno, o ViewModel reaberto recarrega o histórico;
    - `CancellationException` do próprio turno não vira mensagem de erro.
  - Teste do `ChatTurnTracker`: `begin/end` por chave, chaves independentes, `end` sem `begin` é no-op.
- **Instrumentados / UI:**
  - `core:ui`: `CestouModalBottomSheet` sob `CestouTheme` claro e escuro, conteúdo localizado por `testTag`.
  - O módulo `feature:chat` não tem `androidTest`; o fluxo de voltar fica em Maestro (validação manual, Q1).
- **Validação manual (Maestro com `testTag`):** percorrer todos os sheets de §1 em light e dark e o voltar do Chat nos cenários de §6.
- **Comandos:** `./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout` e `:core:ui:connectedDebugAndroidTest`.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Hipótese do voltar incorreta | média | correção ineficaz | reproduzir antes (§7); alternativa: mover o `BackHandler` para dentro do conteúdo do sheet |
| Permitir `Hidden` abre brecha para fechar o chat em andamento (resposta de IA pendente) | média | perda de contexto da conversa | resolvido pelo item 3: a resposta continua e é salva no histórico |
| Dois turnos concorrentes (chat reaberto) corrompem o `AiSession` singleton e sobrescrevem o histórico (`persistHistory` salva a lista completa do ViewModel antigo) | alta | perda de mensagens e histórico inconsistente | `ChatTurnTracker` bloqueia envio por chave; ao fim do turno, mesclar/recarregar o histórico antes de salvar; testes dedicados |
| Chamada que nunca retorna (rede pendurada) mantém o turno vivo para sempre | média | rastreador preso e chat reaberto travado em `isLoading` | confirmar timeouts de rede dos clientes de IA (`core:ai` não define `withTimeout`); se não houver, adicionar um teto generoso (Q7) |
| Custo: tools com efeito colateral (gravar produto no Firestore) agora completam com o chat fechado | média | mudança de comportamento visível | é o comportamento desejado; sem repetição de tool (`toolExecutedThisTurn`, `AiSession.kt`) |
| Turno pendente após logout/troca de conta | baixa | resposta gravada no histórico errado | cancelar turnos pendentes no logout (Q6) |
| Atraso entre retomada e histórico: o usuário não sabe que a resposta chegou | média | UX | sem notificação neste plano (Q5) |
| Cards `surface` sobre `background` perdem contraste no claro | média | leitura | revisar Cart, Editar lista e Products; ajustar o conteúdo, não o container |
| Sheets antes brancos ficam levemente acinzentados | alta | visual; é o objetivo | aceito pelo pedido |
| Chat tem gesto, inset e altura animada especiais | média | regressão de UX | wrapper repassa todos os parâmetros; teste manual |
| `onClose()` do CSAT disparado duas vezes | baixa | métrica duplicada | verificar `AiChatViewModel.onClose` ser idempotente |
| Branch atual tem alterações em andamento (`ChatGraph.kt` etc.) | média | conflito | implementar em branch própria a partir de `develop` |

## 8.1 Pendências registradas na revisão
- `ChatHistoryRepository.load(shoppingId)` e `save(shoppingId, ...)` ficaram sem uso em produção (o chat grava só por `saveSession`). Mantidos: removê-los altera contrato público e exige registro no release (AGENTS.md §6). Candidatos a limpeza em tarefa própria.
- Chamada de IA que nunca retorna mantém o turno (e o `awaitIdleTurns` de novos chats) preso; depende dos timeouts de rede dos clientes, ainda não verificados (Q7).
- Validação manual (Maestro, light e dark) e `:core:ui:connectedDebugAndroidTest` pendentes: sem dispositivo na implementação.

## 9. Perguntas em Aberto (bloqueantes)
Nenhuma bloqueante. Decisões já registradas: Q1 validação manual; Q2 dialogs fora.
- [x] Q3 (resolvida pelo usuário): a resposta da IA nunca é cancelada pelo fechamento; roda desacoplada em segundo plano. O voltar fecha o modal normalmente.
- [x] Q4 (resolvida: sim, criar sessão no primeiro envio) (chat sem contexto): um chat aberto sem lista nem sessão (`persistHistory` hoje não salva nada) deve criar uma sessão (`createNewSession`) ao primeiro envio, para a resposta tardia ser gravada? Recomendação: sim.
- [x] Q5 (adotada a recomendação: fora do plano) (não bloqueante): avisar o usuário quando a resposta chegar com o chat fechado (notificação ou indicador no FAB)? Recomendação: fora deste plano.
- [x] Q6 (adotada a recomendação: sim) (não bloqueante): no logout, descartar turnos pendentes? Recomendação: sim, cancelar o scope dos turnos do usuário anterior.
- [x] Q7 (adotada a recomendação: sem teto novo) (não bloqueante): impor um teto de tempo ao turno (ex.: 2 min)? Hoje não há `withTimeout` em `core:ai`. Recomendação: depender dos timeouts de rede dos clientes e só criar o teto se eles não existirem.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (`architecture/` não cobre tema/UI/chat; lacuna registrada).
- [x] Nenhum código de produção escrito (apenas assinatura).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Símbolo público novo refletido como `additive` e justificado (§4).
- [x] Sem endpoint, remote-config ou cache.
- [x] Perguntas bloqueantes exauridas.
