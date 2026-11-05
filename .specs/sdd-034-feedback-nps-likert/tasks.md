# Task: Feedback no app (NPS, Likert por recurso e polegar nas respostas da IA)

- **Slug:** feedback-nps-likert
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-03
- **Status:** ready (decisões P1-P3 respondidas em 2026-11-03; ver seção 9)
- **Versão-alvo:** 2.4.0 (minor por feature; se for lançado junto com o sdd-033, entra na 2.3.0)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive (`core:domain` ganha `FeedbackPromptRequester`/`FeedbackMoment`; `core:navigation` ganha a rota `FeedbackSurvey`; `core:analytics` ganha eventos e parâmetros; nada existente muda de assinatura)

## 1. Contexto e Motivação
- Pedido do usuário: saber o que os clientes acham do app, com NPS e escala Likert, polegar (binário) nas respostas do chat e satisfação medida **por recurso** no GA. Na loja há só 1 avaliação satisfeita.
- Hoje não existe nenhuma pergunta de satisfação no app. O único canal é o e-mail de "Sugestão/Feedback" em Configurações (`SettingsViewModel.kt:104`, rota `SupportFeedback` em `SettingsViewModel.kt:294`), que exige que o cliente tome a iniciativa.
- Retenção cai a ~0% depois da primeira semana e a base real é pequena (memória do projeto: 2 contas reais, ~950 testers). Qualquer pesquisa precisa ser curta, rara e contextual, e os números serão indicativos, não estatísticos.
- **Decisões do usuário (2026-11-03):**
  - **Sem comentário livre.** Só nota e motivos de uma lista fechada. Combina com a regra já fixada no sdd-033: nenhum texto livre e nenhum conteúdo de mensagem da IA sai do aparelho.
  - **Medição via GA** (eventos enumerados), segmentável por recurso, plano e idioma. Sem coleção nova no Firestore.
  - **Ligado ao uid:** o `user_id` do GA já é o UID do Firebase Auth (sdd-033, P2). Nada novo a gravar; o cliente é localizado no console como hoje.
  - **Pedir avaliação na Play Store** com a In-App Review API (ver risco R1 sobre a política da Play).
- Rastreabilidade: `FinishPurchaseViewModel.kt:69-77` (momento de valor da compra), `AiChatViewModel.kt:172,195` (turno da IA, provider e tool calls), `ChatBubble.kt:38` (bolha do assistente), `ChatMessage.kt:5` (mensagem sem metadados), `MainActivity.kt:80,152-159` (foreground e observação do back stack), `AppUsageTracker.kt` (SharedPreferences de uso), `FirstTimeEventTracker.kt` (flags síncronas), `MobileRoutes.kt:72` (rotas públicas como `Paywall`), `RemoteConfigKeys.kt` (flags), `AnalyticsUserProperties.kt`.

## 2. Escopo
- **Inclui:**
  - **Polegar** ao fim de cada resposta do assistente no chat (👍/👎); no 👎, chips de motivo de lista fechada (opcional, 1 toque).
  - **Likert de 5 pontos (CSAT)** contextual, 1 pergunta, por recurso: `purchase` (ao finalizar compra), `ai_chat` (ao fechar o chat com ≥ 2 respostas) e `photo_scan` (ao confirmar itens do scan).
  - **NPS (0-10)** relacional, 1 pergunta, na tela de listas depois de uso suficiente.
  - Um coordenador que decide **se e quando** perguntar (cooldowns, frequência, telas proibidas, opt-out de analytics, flags remotas).
  - Evento de analytics por interação, mais a user property `nps_class` (promotor, neutro, detrator) para segmentar qualquer relatório por satisfação.
  - Pedido de avaliação na Play Store (In-App Review), desacoplado da pesquisa (ver R1).
  - Atalho "falar com a gente" por e-mail para notas baixas (reaproveita o canal existente; o texto é escrito no app de e-mail do cliente, nunca no nosso).
  - Strings pt-BR, en e es; `testTag` em tudo; testes; textos legais; `CHANGELOG`.
- **Exclui:**
  - Comentário livre dentro do app, qualquer texto de usuário em Firestore/GA, e qualquer anexo de mensagem da IA a um feedback.
  - Nova coleção Firestore ou mudança em `firestore.rules`.
  - Votos do polegar persistidos entre aberturas do chat (ficam só na sessão da tela; ver seção 8).
  - Módulo `:wear`.
  - Pesquisa por e-mail/push, A/B de textos, dashboard do GA (as Explorations são montadas à mão depois).

## 3. Impacto Arquitetural
- **Módulos:** novo `:feature:feedback` (Clean Arch + MVI), `core:domain` (contrato), `core:navigation` (rota pública), `core:analytics` (catálogo), `core:remote-config` (flags), `feature:chat` (polegar), `feature:cart` e `feature:products` (gatilhos), `app` (host do prompt), `settings.gradle.kts`, `feature:subscription` não é tocado.
- **Padrão:** o contrato fica em `core:domain` (como `AnalyticsTracker`); as features só dizem "aconteceu um momento" e o `:feature:feedback` decide. Nenhuma feature depende de outra (`AGENTS.md` §2). O `:app` observa um estado do contrato e navega para uma rota pública, sem conhecer `:feature:feedback`.
- **Novos arquivos:**
  - `core/domain/.../feedback/FeedbackMoment.kt`, `FeedbackPrompt.kt`, `FeedbackPromptRequester.kt`
  - `core/navigation/.../mobile/MobileRoutes.kt` (acrescenta `FeedbackSurvey`)
  - `core/analytics/.../model/AnalyticsScreen.kt` (+ `FEEDBACK_SURVEY`), `AnalyticsEvents.kt`, `AnalyticsParams.kt`, `AnalyticsUserProperties.kt` (editados)
  - `feature/feedback/build.gradle.kts`
  - `feature/feedback/src/main/java/br/com/brunocarvalhs/howmuch/feature/feedback/`
    - `FeedbackNavGraph.kt` (`FeatureNavGraph` com `screenNames`)
    - `domain/model/SurveyKind.kt`, `domain/policy/FeedbackPolicy.kt`, `domain/repository/FeedbackStateStore.kt`
    - `data/store/FeedbackStateStoreImpl.kt` (SharedPreferences), `data/review/PlayReviewLauncher.kt`
    - `domain/usecase/SubmitNpsUseCase.kt`, `SubmitCsatUseCase.kt`, `DismissSurveyUseCase.kt`
    - `presentation/coordinator/FeedbackPromptCoordinator.kt` (implementa `FeedbackPromptRequester`)
    - `presentation/viewmodel/FeedbackSurveyViewModel.kt`, `presentation/intent/FeedbackSurveyIntent.kt`, `presentation/state/FeedbackSurveyUiState.kt`
    - `presentation/screen/FeedbackSurveyScreen.kt`, `presentation/components/LikertScale.kt`, `NpsScale.kt`
    - `navigation/FeedbackGraph.kt`, `di/FeedbackModule.kt`
  - `feature/chat/.../presentation/component/MessageFeedbackBar.kt`
  - Testes correspondentes (seção 7).
- **Diagrama:**

```
 FinishPurchaseVM ─┐                               ┌──► AnalyticsTracker (nps_submitted, csat_submitted,
 AiChatVM (close) ─┼─► FeedbackPromptRequester ─┐  │       feedback_prompt_*, ai_message_feedback)
 ProductPhotoVM ───┘   (core:domain, interface) │  │
 MainActivity.onForeground ────────────────────►│  ├──► UserProperty nps_class
                                                ▼  │
                                  FeedbackPromptCoordinator ──► FeedbackPolicy ◄── FeedbackStateStore (SharedPrefs)
                                   (feature:feedback)               ▲                 RemoteConfig flags
                                        │ pending: StateFlow<FeedbackPrompt?>
                                        ▼
                  MainActivity: pending != null && destino liberado ──► navigate(FeedbackSurvey(kind, feature))
                                        │                                    (rota pública, core:navigation)
                                        ▼
                         FeedbackSurveyScreen ─► ViewModel ─► Submit*UseCase ─► Tracker + store.cooldown
                                        │ (nota alta + elegível)
                                        └──► PlayReviewLauncher (In-App Review)

 AiChat: ChatBubble(assistente) ─► MessageFeedbackBar ─► AiChatViewModel ─► ai_message_feedback (sem texto)
```
- **Hilt:** `FeedbackPromptCoordinator` `@Singleton`, `@Binds` para `FeedbackPromptRequester` em `FeedbackModule`; `FeedbackNavGraph` entra no `Set<FeatureNavGraph>` como as demais features (`ChatNavGraph.kt:16`, `@Inject constructor`). `FeedbackStateStore` e `PlayReviewLauncher` injetados no coordinator e no ViewModel; ninguém instancia à mão (`AGENTS.md` §5).
- **Por que SharedPreferences e não DataStore:** mesma decisão do `FirstTimeEventTracker` e do `AppUsageTracker`: flags e carimbos minúsculos, lidos de forma síncrona numa decisão de UI que não pode suspender. Cache em memória carregado uma vez; escrita com `apply()`.
- **Por que não Firestore:** não há texto para guardar. Notas enumeradas no GA bastam, e evitam ampliar a superfície de dados pessoais, a `firestore.rules` (ainda "PROPOSAL — NOT DEPLOYED") e o fluxo de exclusão de conta (`DeleteAccountUseCase.kt`).

## 4. Contratos e Interfaces
> Assinaturas apenas. `public` só onde um módulo diferente consome.

```kotlin
// core:domain — public: consumido por cart, products, chat (emissores) e app (host); justificativa: padrão de AnalyticsTracker
enum class FeedbackMoment { PURCHASE_FINISHED, AI_CHAT_CLOSED, PHOTO_SCAN_CONFIRMED, APP_RELATIONSHIP }

data class FeedbackPrompt(val kind: SurveyKind, val feature: String?)   // kind: NPS | CSAT
enum class SurveyKind { NPS, CSAT }

interface FeedbackPromptRequester {
    val pending: StateFlow<FeedbackPrompt?>
    fun onAppForeground()                       // conta sessões; sem I/O bloqueante
    fun onMoment(moment: FeedbackMoment)        // ex.: depois de finalizar compra
    fun consume(prompt: FeedbackPrompt)         // o app mostrou; limpa o pendente
}

// core:navigation — public (rota entre módulos, como Paywall em MobileRoutes.kt:72)
@Serializable data class FeedbackSurvey(val kind: String, val feature: String?) : NavKey, RouteProtocol
```

```kotlin
// feature:feedback — internal
internal class FeedbackPolicy(config: FeedbackConfig, clock: () -> Long) {
    fun evaluate(moment: FeedbackMoment, state: FeedbackState, ctx: PromptContext): FeedbackPrompt?
    fun shouldRequestStoreReview(state: FeedbackState): Boolean
}
internal interface FeedbackStateStore {
    fun load(): FeedbackState
    fun recordShown(prompt: FeedbackPrompt)
    fun recordAnswered(prompt: FeedbackPrompt, score: Int)
    fun recordDismissed(prompt: FeedbackPrompt)
    fun recordSession()
    fun recordStoreReviewRequested()
}
internal data class FeedbackSurveyIntent(
    val onScoreSelected: (Int) -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onContactSupport: () -> Unit = {},
)
```

```kotlin
// feature:chat — internal
data class MessageVote(val value: Vote, val reason: ReasonChip? = null)   // Vote: UP | DOWN
enum class ReasonChip { WRONG_ANSWER, NOT_UNDERSTOOD, TOO_SLOW, DID_NOTHING, OTHER }
// AiChatUiState ganha: val votes: ImmutableMap<Long, MessageVote>
//                      val turns: ImmutableMap<Long, AiTurnInfo>   // provider, toolCalls; só em memória
// AiChatIntent ganha: onMessageVote: (messageId: Long, vote: Vote) -> Unit,
//                     onMessageVoteReason: (messageId: Long, reason: ReasonChip) -> Unit
```

- **Eventos de analytics (nomes canônicos em `AnalyticsEvents`, todos com KDoc de uma linha dizendo o que revelam; nenhum texto livre):**

| Evento | Parâmetros | O que revela |
|---|---|---|
| `feedback_prompt_shown` | `kind` (nps/csat), `feature`, `trigger` | quantas vezes perguntamos e por quê |
| `feedback_prompt_dismissed` | `kind`, `feature` | quem fecha sem responder (fadiga) |
| `nps_submitted` | `score` (0-10), `value` (promoter/passive/detractor) | satisfação geral |
| `csat_submitted` | `feature`, `score` (1-5) | satisfação por recurso (satisfeito = ≥ 4) |
| `ai_message_feedback` | `value` (up/down), `provider`, `tool_calls`, `reason` (chip, só no 👎) | qualidade percebida por agente |
| `store_review_requested` | `trigger` | quando pedimos avaliação na loja |
| `feedback_contact_opened` | `kind`, `feature` | quem quis falar com a gente |

- **Parâmetros novos:** só `score` (inteiro). O resto reutiliza o que o sdd-033 já registrou (`feature`, `value`, `provider`, `tool_calls`, `reason`, `trigger`), para não gastar dimensões personalizadas do GA4 (limite 50).
- **User property nova:** `nps_class` (`promoter`/`passive`/`detractor`), atualizada ao responder o NPS. Entra no `AnalyticsUserProperties` (hoje ~13 de 25).
- **Remote Config (`RemoteConfigKeys.kt`):** `feat_feedback_enabled` (default true), `feedback_nps_cooldown_days` (90), `feedback_csat_cooldown_days` (30), `feedback_store_review_enabled` (true).
- **Dependência nova:** `com.google.android.play:review-ktx` em `gradle/libs.versions.toml` (versão a fixar na implementação), usada só em `:feature:feedback`.
- **Sem novo endpoint, coleção Firestore ou regra de segurança.**

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | Nenhuma pergunta de satisfação no app | NPS, CSAT e polegar existem, todos opcionais e dispensáveis | `AGENTS.md` §2; busca por "nps", "csat", "rating" sem resultado |
| 2 | Texto livre e conteúdo de IA nunca vão ao GA | Mantido: parâmetros só de enum, bucket e inteiro; nenhum texto de mensagem ou resposta em nenhum evento | sdd-033 §5 regras 5 e 8; decisão do usuário |
| 3 | Analytics tem opt-out (Configurações > Dados) | Com a coleta OFF, **nenhuma pesquisa é exibida** e nenhum evento sai; o polegar some | `AppSettings.kt:20` (`analyticsEnabled`) |
| 4 | Compra finalizada notifica membros e volta (`goBack`) | Depois do sucesso, emite `onMoment(PURCHASE_FINISHED)`; a pesquisa aparece na próxima tela estável, nunca durante o fluxo | `FinishPurchaseViewModel.kt:69-80` |
| 5 | Chat registra turno (provider, tool calls) só como evento | O mesmo turno é guardado em memória por mensagem para o polegar anexar `provider` e `tool_calls` | `AiChatViewModel.kt:172,195` |
| 6 | Mensagem do chat não tem metadados | `ChatMessage` **não muda** (modelo de domínio e histórico em disco intactos); votos e turnos ficam no estado da tela | `ChatMessage.kt:5` |
| 7 | Rotas entre módulos ficam em `MobileRoutes` | `FeedbackSurvey` segue o padrão de `Paywall`; `screenNames` ganha a entrada (teste Konsist do sdd-033 falha o build se faltar) | `MobileRoutes.kt:72`, `ArchitectureTest.kt` |
| 8 | Avaliação na loja nunca é pedida | Pedido via In-App Review, no máximo 1 a cada 120 dias, desacoplado da pesquisa, nunca para quem deu nota baixa recente | decisão do usuário; risco R1 |

### Política de exibição (parâmetros em Remote Config quando indicado)
- **Nunca** se: `analyticsEnabled=false`, flag `feat_feedback_enabled=false`, conta com menos de 1 dia, já houve **qualquer** pesquisa nos últimos 7 dias, ou já foi mostrada 1 nesta sessão.
- **Telas proibidas** (o app não interrompe): fluxo de auth, `CompleteName`, carrinho e finalização de compra em andamento, formulários de produto, `AiChat` com `isLoading`, paywall, scanner/QR, teclado aberto.
- **CSAT por recurso:** no máximo 1 por recurso a cada `feedback_csat_cooldown_days` (30). Gatilhos: `PURCHASE_FINISHED` (1ª ou 3ª compra finalizada, depois a cada cooldown), `AI_CHAT_CLOSED` (≥ 2 respostas recebidas na sessão do chat), `PHOTO_SCAN_CONFIRMED` (≥ 1 item confirmado).
- **NPS:** no máximo 1 a cada `feedback_nps_cooldown_days` (90). Só em `APP_RELATIONSHIP`: ≥ 7 dias desde o 1º uso **e** ≥ 3 sessões, e só ao chegar na tela de listas.
- **Fechou sem responder:** respeita backoff: 14 dias na 1ª vez, 45 na 2ª, 90 depois. Não conta como resposta.
- **Prioridade:** se NPS e CSAT estão elegíveis juntos, vale o CSAT do momento (contextual); o NPS espera.

## 6. Critérios de Aceitação
- **Funcional:**
  - Cada resposta do assistente mostra 👍/👎; 👎 abre chips opcionais; após votar a barra trava (1 voto por mensagem na sessão).
  - Finalizar uma compra com a política liberando mostra o Likert na tela seguinte, não no meio do fluxo; nota 1-5 emite `csat_submitted` e agradece.
  - NPS aparece só nas condições da política e emite `nps_submitted` com a classe correta (0-6 detrator, 7-8 neutro, 9-10 promotor) e atualiza `nps_class`.
  - Fechar sem responder emite `feedback_prompt_dismissed` e aplica o backoff.
  - Nota ≤ 6 (NPS) ou ≤ 2 (CSAT) oferece "falar com a gente" por e-mail; nota alta **não** abre a avaliação na loja na mesma hora (ver R1).
  - Com analytics OFF nada aparece; religar a coleta volta a permitir as pesquisas, respeitando cooldowns já gravados.
- **Privacidade:** nenhum evento contém texto digitado, texto de mensagem/resposta, e-mail ou nome. Teste de arquitetura e de unidade garantem que os mapas de parâmetros só têm `String` de enum, `Int` e `Boolean`.
- **Performance:**
  - A política roda em O(1) sobre estado em memória, só em lambdas de intent e quando o destino muda; zero trabalho em recomposição.
  - Estado do store carregado uma vez (lazy) e mantido; escrita com `apply()`; nenhum `Dispatchers.IO` novo na UI.
  - `FeedbackSurveyScreen` só compõe quando a rota está no back stack; `MessageFeedbackBar` recebe `MessageVote?` imutável por mensagem (`key = message.id`), então votar numa bolha não recompõe a lista inteira.
  - `PlayReviewLauncher` pede `ReviewInfo` só quando elegível e com timeout; falha vai para o `CrashReporter` e nunca bloqueia a UI.
- **Feature toggle:**

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `feat_feedback_enabled` | true | remote-config | nenhuma pesquisa; polegar escondido | tudo conforme a política |
  | `feedback_store_review_enabled` | true | remote-config | nunca pede avaliação na loja | pede conforme `shouldRequestStoreReview` |
  | `analytics_collection_enabled` | true | DataStore (sdd-033) | nada é exibido nem enviado | pesquisas habilitadas |

- **Observabilidade:** eventos canônicos acima; `feedback_prompt_shown` permite calcular a taxa de resposta (`submitted / shown`). Exploration sugerida no GA: nota média por `feature`, NPS por `plan` e por `app_language`, e `ai_message_feedback` por `provider`.
- **Acessibilidade e i18n:** cada opção com área de toque ≥ 48dp e `contentDescription` ("3 de 5, neutro"); escala Likert com rótulos nas pontas e emoji apenas decorativo; todas as strings em `strings.xml` pt-BR, en e es (`feature/settings/src/main/res/values-es` já existe como precedente); nenhum texto literal em Composable.
- **Compatibilidade:** minSdk 28; In-App Review exige Play Services (sem ele, o launcher falha em silêncio e nada é mostrado); dados locais só em SharedPreferences; sem migração.
- **Privacidade e lojas:** `docs/legal/privacy.html` (pt e en) ganha uma frase: respostas de satisfação (nota e recurso) associadas ao identificador interno; a Data Safety da Play deve ser conferida pelo maintainer, pois "app interactions" já é declarado pelo Analytics (a confirmar).

## 7. Plano de Testes
- **Unitários:**
  - `FeedbackPolicyTest` (relógio fake): analytics OFF; flag OFF; conta nova; cooldown global de 7 dias; cooldown por recurso; NPS exige 7 dias e 3 sessões; backoff 14/45/90 após dispensar; 1 por sessão; prioridade CSAT sobre NPS; tela proibida; `shouldRequestStoreReview` (120 dias, sem nota baixa recente).
  - `FeedbackStateStoreImplTest` (Robolectric): persistência de carimbos, contagem de sessões e dispensas; instância nova lê o mesmo estado.
  - `FeedbackPromptCoordinatorTest`: `onMoment` elegível publica em `pending`; inelegível não publica; `consume` limpa; `onAppForeground` conta sessão; flag OFF e analytics OFF suprimem.
  - `FeedbackSurveyViewModelTest`: NPS 0/6/7/8/9/10 → classe e `nps_class`; CSAT 1..5; dispensar; "falar com a gente" emite `feedback_contact_opened`; nenhum mapa de parâmetros contém texto livre.
  - `AiChatViewModelTest` (acréscimos): 👍 e 👎 emitem `ai_message_feedback` com `provider` e `tool_calls` do turno; 👎 + chip adiciona `reason`; segundo voto na mesma mensagem é ignorado; voto sem turno conhecido não quebra; analytics OFF esconde a barra.
  - `FinishPurchaseViewModelTest`, `ProductPhotoViewModelTest`, `AiChatViewModelTest` (fechar chat): chamam `FeedbackPromptRequester.onMoment` com o momento certo, e **não** chamam no caminho de falha.
  - `PlayReviewLauncherTest`: sem Play Services não lança; timeout vai ao `CrashReporter`.
- **Arquitetura (Konsist, `ArchitectureTest.kt`):** `FeedbackSurvey::class` está em algum `screenNames` (regra existente); `:feature:feedback` não importa outras features; novos eventos em `AnalyticsEvents` com nomes únicos, snake_case, ≤ 40 caracteres (regra existente); `*Intent` do módulo coberto (regra D4 do sdd-033).
- **Instrumentados / UI (por `testTag`, sem assert de texto):** `feedback_score_{n}` na escala, `feedback_dismiss`, `feedback_contact`, `message_vote_up`, `message_vote_down`, `message_vote_reason_{chip}`; fluxo: finalizar compra → pesquisa → nota → agradecimento. Comando: `./gradlew :app:connectedDebugAndroidTest :feature:feedback:connectedDebugAndroidTest`.
- **Manual:** DebugView do GA4 (`adb shell setprop debug.firebase.analytics.app br.com.brunocarvalhs.howmuch`) cobrindo os 7 eventos; forçar o In-App Review com a internal app sharing da Play; conferir os cooldowns avançando o relógio do aparelho.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`. Sem `@Suppress` (regra do projeto).

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| **R1** A Play recomenda **não** condicionar o pedido de avaliação a uma pergunta de sentimento ("você gostou do app?"); usar nota alta do NPS como porta de entrada pode ferir as diretrizes da In-App Review | média (a conferir na doc vigente antes de implementar) | alto (risco de política da Play) | Desacoplar: o pedido de avaliação é disparado por **marcos de uso** (ex.: 3ª compra finalizada e ≥ 14 dias), nunca logo após a pesquisa; a nota só **suprime** o pedido para quem foi detrator nos últimos 30 dias. Conferir o texto atual das diretrizes e ajustar `shouldRequestStoreReview` se for mais restritivo |
| **R2** Fadiga de pesquisa derruba a retenção, já frágil | média | médio | cooldowns e backoff, 1 por sessão, telas proibidas, flag remota para desligar tudo |
| **R3** Base pequena torna o NPS ruidoso | alta | baixo | tratar n < 30 como indicativo; olhar CSAT por recurso e o polegar por provider, que acumulam mais rápido |
| **R4** Dado pessoal em evento | baixa | alto | só enums e inteiros, testes de ausência de texto, revisão de maintainer |
| **R5** Perguntar com o analytics desligado violaria o opt-out | baixa | alto | política suprime tudo quando `analyticsEnabled=false` |
| **R6** Voto do polegar perdido ao reabrir o chat (não persistido) | alta | baixo | aceito: é sinal por sessão; persistir exigiria mudar `ChatMessageModel` e o banco do histórico, fora do escopo |
| **R7** Pesquisa sobrepõe uma ação do cliente ao aparecer | média | médio | só abre em destino estável e liberado; fecha por gesto de voltar sem penalidade |
| **R8** Nova dependência `play-review` | baixa | baixo | uso isolado em `PlayReviewLauncher`, atrás de flag; sem Play Services o app segue normal |

## 9. Perguntas em Aberto (bloqueantes)
- [x] **P1 - Comentário livre?** Respondida em 2026-11-03: **não**. Só nota e motivos de lista fechada. Satisfação medida via GA, por recurso.
- [x] **P2 - Identidade:** ligado ao uid, já garantido pelo `user_id` do GA (sdd-033). Nada novo a gravar.
- [x] **P3 - Play Store:** sim, In-App Review, com o desenho desacoplado da pesquisa descrito em R1.
- [ ] (não bloqueante) **Confirmar a política vigente da Play** sobre pedir avaliação depois de uma pergunta de sentimento, antes de implementar `shouldRequestStoreReview`. O plano já assume o desenho mais conservador.
- [ ] (não bloqueante) **Limiares iniciais** (7 dias, 3 sessões, 1ª/3ª compra, ≥ 2 respostas de IA): padrões razoáveis, ajustáveis por Remote Config sem nova versão; revisar depois de 2 semanas de dados.

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (e `architecture/` onde existe; `architecture/04-integracoes-externas.md` deve ganhar a nota sobre a In-App Review na implementação).
- [x] Nenhum código de produção escrito (apenas assinaturas e diagramas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Impacto público declarado como `additive` (novo contrato em `core:domain`, nova rota em `core:navigation`, novos eventos); nada existente muda.
- [x] Novo comportamento remoto/coleta: toggles definidos (`feat_feedback_enabled`, `feedback_store_review_enabled`, opt-out de analytics) e observabilidade planejada.
- [x] Novo cache justificado: SharedPreferences com espelho em memória (flags síncronas minúsculas, precedente no `FirstTimeEventTracker`); sem DataStore, Room ou Firestore.
- [x] Novo módulo: ao implementar, atualizar `AGENTS.md` §2 e §4, `.agents/skills/android-architecture/SKILL.md`, `settings.gradle.kts` e o `README` se citar módulos (`AGENTS.md` §3).
- [x] Perguntas bloqueantes exauridas (restam apenas 2 não bloqueantes).
