# Task: Analytics forte em todo o app (telas com nome real, ações, IA e comportamento)

- **Slug:** analytics-forte
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-02
- **Status:** ready (P1-P3 respondidas em 2026-11-02)
- **Versão-alvo:** 2.3.0 (minor por feature)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive (`FeatureNavGraph`, `AnalyticsEvents`/`AnalyticsParams`, extensões de `AnalyticsTracker`; nenhuma assinatura existente muda)

## 1. Contexto e Motivação
- Pedido do usuário: analytics forte em todo o app (abertura, clique, atualização, exclusão e outros), nome da tela no relatório do GA em vez do nome da ViewModel, eventos das mensagens da IA e demais eventos para entender o comportamento do app na mão do cliente.
- **Causa raiz do "ViewModel" no GA:** cada ViewModel chama `trackScreenView(screenName = "cart", screenClass = "CartViewModel")` no `init` (ex.: `CartViewModel.kt:94`, `ShoppingListViewModel.kt:153`, `AiChatViewModel.kt:43`; 16 ocorrências). `FirebaseAnalyticsTracker.kt:28-35` grava `screenClass` em `firebase_screen_class`, que é a dimensão "Classe da tela" nos relatórios do GA4 e nas Explorations.
- **Efeitos colaterais do desenho atual (mesma raiz):**
  - O `screen_view` é disparado no `init` da ViewModel: não repete ao voltar para a tela (a ViewModel sobrevive no back stack) e só existe para 16 das ~40 rotas. Faltam, por exemplo, `ThemeSettings`, `LanguageSettings`, `CurrencySettings`, `AiSettings`, `ShoppingSettings`, `DataSettings`, `NotificationSettings`, `EmailSignIn`, `EmailSignUp`, `PasswordReset`, `CompleteName`, `QrCode`, `ProductPickerRoute`, `EditItemRoute`, `ConfirmItemRoute`, `ProductHistoryRoute`, `ShareOptionsRoute`, `LinkWearDevice`.
  - Não há opt-out do reporting automático de Activity (`AndroidManifest.xml` não tem `google_analytics_automatic_screen_reporting_enabled`), então a `MainActivity` entra como tela em paralelo (hipótese: confirmar no DebugView).
- **Cobertura de eventos hoje (`AnalyticsEvents.kt`):** 40 chamadas `trackEvent`, quase só funil de criação/compra/auth-falha. Sem evento para: atualizar produto, alternar "comprado", mover, limpar comprados, editar lista, compartilhar (parcial), login/cadastro com sucesso (só falhas existem: `AUTH_SIGN_IN_FAILED`), troca de tema, fechar/abrir chat, resposta da IA, erro da IA, execução de tool da IA, latência.
- Dois eventos usam string literal fora do objeto central: `"ai_chat_session_created"` (`AiHistoryViewModel.kt:58`) e `"ai_chat_voice_input_started"` (`AiChatViewModel.kt:114`).
- `setUserId`/`setUserProperty` existem no contrato (`AnalyticsTracker.kt`) mas **nenhum chamador** os usa (busca em `core/auth` só acha o `setUserId` do Crashlytics, `FirebaseAuthService.kt:215`). O GA não segmenta por plano, idioma ou tema.
- **Lacuna de conformidade:** `docs/index.html:136` promete "Você pode desligar a coleta de Analytics e de relatórios de erro ... em Configurações > Dados", mas não existe `setAnalyticsCollectionEnabled` nem toggle no código (busca por `setAnalyticsCollectionEnabled`, `collection_enabled` e "analytics" em `feature/settings` retornou só textos legais). Ampliar a coleta sem isso agrava a divergência (ver seção 9, P1).
- `architecture/04-integracoes-externas.md:79,84` marca `core:analytics` como não verificado. Este plano cita o código diretamente e propõe documentar o contrato em `architecture/`.

## 2. Escopo
- **Inclui:**
  - Telas: `screen_view` único e central, disparado em toda mudança de destino (inclui dialogs), com `screen_name` canônico e `screen_class` = nome do Composable da tela. Remover todos os `trackScreenView` das ViewModels.
  - Desligar o `screen_view` automático da Activity.
  - Taxonomia de eventos: ações (clique relevante, criar, atualizar, excluir, mover, alternar, compartilhar, buscar), auth com sucesso, assinatura, configurações, erros de operação.
  - IA: mensagens enviadas/recebidas, voz, sessões, latência, erro, execução de tool, bloqueio Pro, fallback de provider, uso do trial. **Nunca** o conteúdo.
  - User properties (plano, idioma, moeda, tema, provider de IA) e `user_id` (sujeito à pergunta P2).
  - Endurecimento do `FirebaseAnalyticsTracker` (limites do Firebase, nomes reservados).
  - Toggle de opt-out em Configurações > Dados (P1 aprovada).
  - Testes unitários, regra Konsist que impede regressão, doc em `architecture/` e `CHANGELOG`.
- **Exclui:**
  - Módulo `:wear` (tem dependência própria de `firebase.analytics`, `wear/build.gradle.kts:113`; plano separado).
  - Eventos dentro de `Composable` por recomposição, e-mail ou qualquer PII no GA, session replay, heatmap, A/B testing e Remote Config de analytics.
  - Exportação BigQuery e dashboards do GA (as Explorations existentes são mantidas; ver memória do projeto).
  - Renomear eventos já existentes (preserva as Explorations e o histórico no GA).

## 3. Impacto Arquitetural
- **Módulos:** `core:analytics` (contrato, catálogo, tracker), `core:navigation` (`FeatureNavGraph`), `app` (listener de destino, manifest), todas as `feature:*` (presentation/di), `feature:ai-agent` (data), `feature:settings` (toggle).
- **Padrão:** Clean Arch + MVI. Analytics fica na camada de presentation (ViewModel/intent) e, para a IA, também em `data` do `ai-agent` (único chokepoint de dispatch). Nenhuma feature depende de outra.
- **Decisão central, tela:** tracking **centralizado por destino**, não por ViewModel.
  - Cada `FeatureNavGraph` passa a declarar `screens: Map<KClass<*>, AnalyticsScreen>` (rotas `internal` continuam visíveis, pois o mapa é montado dentro do módulo da feature).
  - `MainActivity` já observa o back stack (`currentBackStackEntryAsState`, `MainActivity.kt:~135`); um `ScreenTracker` no `app` resolve o destino com `hasRoute(kclass)` (mesma técnica de `RouteProtocol.kt:isProtectedRoute`) e chama `trackScreenView`.
  - Alternativas descartadas: (a) `LaunchedEffect` em cada `composable<>`: 40 pontos de manutenção, esquecível; (b) manter na ViewModel: causa do problema atual.
- **Decisão central, ações:** dois níveis para respeitar os limites do GA4 (500 nomes de evento distintos, 25 parâmetros por evento, 50 dimensões personalizadas de evento):
  1. **Eventos de domínio** específicos onde já existem (mantidos) ou onde o funil importa (`product_updated`, `list_updated`...).
  2. **Evento genérico `ui_action`** para cliques de navegação/controle sem valor de funil próprio, com `element_id` vindo de um catálogo fechado (`AnalyticsElements`). Evita explosão de nomes e permite um único relatório "onde as pessoas clicam".
- **Novos arquivos:**
  - `core/analytics/.../model/AnalyticsScreen.kt`, `AnalyticsElements.kt`, `AnalyticsUserProperties.kt`
  - `core/analytics/.../util/AnalyticsSanitizer.kt`
  - `core/analytics/.../extension/AnalyticsTrackerExtensions.kt` (`trackAction`, `trackMutation`, `trackError`)
  - `app/src/main/java/br/com/brunocarvalhs/howmuch/analytics/ScreenTracker.kt`
  - `app/src/main/java/br/com/brunocarvalhs/howmuch/analytics/UserPropertiesSync.kt`
  - `feature/ai-agent/.../data/service/AiTelemetry.kt` (interna)
  - `feature/settings/.../domain/usecase/SetAnalyticsConsentUseCase.kt` 
  - Testes correspondentes.
- **Diagrama:**

```
 NavHost (MainActivity)
   currentBackStackEntry ──► ScreenTracker ──► AnalyticsTracker.trackScreenView(name, class)
                                  ▲                      │
        Set<FeatureNavGraph>.screens (Hilt multibinding) │
        (Cart, Shopping, Auth, Settings, Chat, ...)      ▼
                                              FirebaseAnalyticsTracker ─► Sanitizer ─► FirebaseAnalytics
 ViewModel intents ──► trackAction / trackMutation / trackError  ───────────┘
 Chat VM   ──► ai_message_sent / ai_response_received / ai_response_failed / ai_voice_*
 BaseAgent ──► ai_tool_executed / ai_tool_blocked_pro      FallbackAgent ──► ai_provider_fallback
 SettingsRepository / Billing / Auth ──► UserPropertiesSync ──► setUserProperty / setUserId
```
- **Hilt:** `ScreenTracker` e `UserPropertiesSync` recebem `Set<FeatureNavGraph>` (já injetado em `MainActivity.kt:62`) e `AnalyticsTracker` (já `@Singleton`, `AnalyticsModule.kt`). `feature:ai-agent` passa a depender de `:core:analytics` (adicionar em `feature/ai-agent/build.gradle.kts:17-20`); `AgentDependencies` recebe o `AnalyticsTracker`.

## 4. Contratos e Interfaces
> Assinaturas apenas. `public` é necessário porque os contratos são consumidos por vários módulos.

```kotlin
// core:analytics — public (consumido por app + todas as features)
enum class AnalyticsScreen(val id: String, val screenClass: String) {
    SHOPPING_LIST("shopping_list", "ShoppingScreen"),
    CART("cart", "CartScreen"),
    AI_CHAT("ai_chat", "AiChatScreen"),
    // ... um por rota; id preserva os nomes já enviados ao GA, screenClass = Composable real
}

// core:navigation — public, additive (default vazio, não quebra implementadores atuais)
interface FeatureNavGraph {
    val screenNames: Map<KClass<*>, ScreenName> get() = emptyMap()
}
data class ScreenName(val id: String, val screenClass: String)
```
- **Nota:** o `FeatureNavGraph` expõe só `ScreenName` (tipo próprio de navegação) e o `app` traduz para `trackScreenView`. O enum `AnalyticsScreen` fica no `core:analytics`; `core:navigation` depende de `core:analytics` apenas para `AnalyticsScreen.toScreenName()` (`ScreenNames.kt`), sem ciclo, e cada `*NavGraph` converte `AnalyticsScreen` em `ScreenName`.
- `AnalyticsTracker` (interface) ganhou `setCollectionEnabled(enabled)` e `val isCollectionEnabled` (opt-out de P1; a única implementação é `FirebaseAnalyticsTracker`, mocks relaxed seguem compilando). O resto entra como extensões (mantém mocks atuais compilando):
  - `fun AnalyticsTracker.trackAction(element: AnalyticsElement, extra: Map<String, Any?> = emptyMap())` → `ui_action` com `element_id`.
  - `fun AnalyticsTracker.trackMutation(event: String, entity: AnalyticsEntity, extra...)` → exige `entity` (`PRODUCT`, `SHOPPING_LIST`, `CHAT_SESSION`...).
  - `fun AnalyticsTracker.trackError(operation: String, error: Throwable)` → `operation_failed` com `operation` e `error_type` (classe da exceção; **nunca** `message`, pode conter dado pessoal).
- **Novos eventos (nomes canônicos, snake_case, ≤ 40 chars, em `AnalyticsEvents`):**

| Grupo | Evento | Parâmetros (sem PII) |
|---|---|---|
| Tela | `screen_view` (nativo) | `screen_name`, `screen_class` |
| Ação genérica | `ui_action` | `element_id`, `screen_name` implícito |
| Lista | `shopping_list_updated`, `shopping_list_opened` | `shopping_id`, `field_changed` |
| Produto | `product_updated`, `product_purchased_toggled`, `product_moved`, `product_quantity_changed`, `cart_purchased_cleared` | `shopping_id`, `product_id`, `source` |
| Compra | `cart_share_options_opened`, `cart_product_history_opened` | `shopping_id` |
| Auth (GA4 recomendados) | `login`, `sign_up`, `password_reset_requested`, `profile_name_completed` | `method` (google/email) |
| Assinatura | `paywall_viewed`, `subscription_started`, `subscription_cancelled_hint` | `source`, `plan` |
| Config | `settings_theme_changed`, `settings_ai_provider_changed`, `settings_notifications_toggled`, `analytics_consent_changed` | valor enumerado, nunca texto livre |
| Busca | `search` (GA4 recomendado, substitui nada; `PRODUCT_SEARCH_PERFORMED` permanece) | `query_length`, `result_count` |
| Erro | `operation_failed` | `operation`, `error_type` |
| **IA** | `ai_chat_opened` | `source` (fab, cart, deeplink) |
| **IA** | `ai_message_sent` (hoje `ai_chat_message_sent`, **mantido**; só ganha parâmetros) | `input_method` (text/voice), `length_bucket` (1-20, 21-80, 81+), `session_message_index`, `has_shopping_context` |
| **IA** | `ai_response_received` | `latency_ms`, `response_length_bucket`, `tool_calls` (int), `provider`, `fallback_used` |
| **IA** | `ai_response_failed` | `error_type` (network/provider_unavailable/generic), `provider` |
| **IA** | `ai_tool_executed` | `action_id`, `success`, `duration_ms`, `provider` |
| **IA** | `ai_tool_blocked_pro` | `action_id` |
| **IA** | `ai_provider_fallback` | `from`, `to` |
| **IA** | `ai_voice_input_started` (nome atual literal, passa a constante), `ai_voice_input_failed` | `reason` (unsupported/permission/recognition) |
| **IA** | `ai_session_created` (hoje `ai_chat_session_created`, **mantido**), `ai_session_selected`, `ai_trial_message_used` | - |
- **Nomes antigos:** `ai_chat_session_created` e `ai_chat_voice_input_started` continuam com o mesmo valor, só viram constantes.
- **User properties** (≤ 25 no GA4, valor ≤ 36 chars, nome ≤ 24):

  | Propriedade | Valores | Fonte |
  |---|---|---|
  | `plan` | `free`, `pro` | `SubscriptionRepository.observeStatus()` (`SubscriptionStatus.kt:10-12`) |
  | `ever_pro` | `true`, `false` | flag em DataStore, vira `true` na 1ª vez que o status é PRO e nunca volta; separa churn de nunca-pagou |
  | `ai_trial_used` | `true`, `false` | `AiTrialRepository` (mesma fonte do `markFreeMessageUsed` em `AiChatViewModel.kt`) |
  | `account_age_bucket` | `0-1d`, `2-7d`, `8-30d`, `30d+` | `creationTimestamp` do usuário do Firebase Auth, recalculado a cada `app_foreground` |
  | `is_tester` | `true`, `false` | instalador diferente de `com.android.vending` (Firebase App Distribution/sideload); a fonte exata deve ser confirmada na implementação |
  | `lists_bucket` | `0`, `1-3`, `4+` | contagem de listas |
  | `app_language`, `currency`, `theme`, `ai_provider` | enumerados | `SettingsRepository` |
  | `has_wear` | `true`, `false` | pareamento com o relógio |
  | `notifications_enabled` | `true`, `false` | permissão do sistema + preferência |

  `plan_period` (mensal/anual) fica fora até confirmar no código de `core:billing` se o plano expõe o período.
- **Dimensões personalizadas a registrar no GA4** (manual, fora do código): `element_id`, `entity`, `source`, `input_method`, `error_type`, `provider`, `action_id`, `latency_ms` (métrica), `plan` (escopo usuário).
- **Sem novos endpoints, coleções Firestore ou remote-config** (exceto o toggle P1, que é local em DataStore).

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | `screen_class` = nome da ViewModel | `screen_class` = nome do Composable; `screen_name` preservado | `FirebaseAnalyticsTracker.kt:28-35`, `CartViewModel.kt:94` |
| 2 | `screen_view` só no `init` da ViewModel | disparado a cada mudança de destino, inclusive volta e dialogs | `CartViewModel.kt:93-96`, `MainActivity.kt` (observa back stack) |
| 3 | `screen_view` automático da Activity ligado | desligado via meta-data no manifest | `app/src/main/AndroidManifest.xml` (ausente) |
| 4 | `AI_CHAT_MESSAGE_SENT` sem parâmetros e disparado antes do envio | mantém nome e ponto de disparo, ganha parâmetros; resposta e erro viram eventos próprios | `AiChatViewModel.kt:154` |
| 5 | Erro de IA só vira mensagem na UI | também emite `ai_response_failed` com `error_type` (sem `e.message`) | `AiChatViewModel.kt:~190-205` |
| 6 | Tool executada sem telemetria | `ai_tool_executed` no único chokepoint, `ai_tool_blocked_pro` no gate | `BaseAgent.kt:executeAction` |
| 7 | Fallback de provider silencioso | `ai_provider_fallback` | `FallbackAgent.kt:catch` |
| 8 | Eventos não carregam PII (valores, estabelecimentos, categorias) | regra mantida e estendida: nenhum texto livre, nenhum conteúdo de mensagem da IA, nenhum preço | comentário em `AnalyticsEvents.kt` (History), `AGENTS.md` §5 |
| 9 | Site promete opt-out de Analytics | toggle real controla `setAnalyticsCollectionEnabled`  | `docs/index.html:136` |
| 10 | `trackEvent` aceita qualquer string | sanitização central (nome ≤ 40, `[A-Za-z][A-Za-z0-9_]*`, sem prefixos `firebase_`/`ga_`/`google_`; valor string ≤ 100; ≤ 25 params) | `FirebaseAnalyticsTracker.kt:trackEvent` |

## 6. Critérios de Aceitação
- **Telas:** no DebugView do GA4, navegar por todas as rotas mostra `screen_name` canônico e `screen_class` terminando em `Screen`; nenhum valor com `ViewModel`; nenhum `screen_view` com `MainActivity`; voltar para uma tela dispara novo `screen_view`.
- **Cobertura:** cada `composable<>`/`dialog<>` de `feature/*/navigation/*Graph.kt` (e `AuthNavGraph.kt:39`) tem entrada em `screenNames` (garantido por teste Konsist, seção 7).
- **Ações:** criar, atualizar, excluir, mover, alternar comprado, limpar comprados, compartilhar e buscar emitem evento com `entity`/`source`; cliques de navegação relevantes emitem `ui_action` com `element_id` do catálogo.
- **IA:** uma conversa de 1 mensagem gera, nessa ordem, `ai_message_sent` → (`ai_tool_executed`)* → `ai_response_received` ou `ai_response_failed`; `latency_ms` medido com `TimeSource.Monotonic`; nenhum parâmetro contém texto da mensagem ou da resposta.
- **Performance:**
  - `logEvent` do Firebase já é assíncrono; o tracker só monta `Bundle` e valida (O(#params), sem I/O).
  - Nenhum evento em corpo de Composable ou recomposição; só em lambdas de intent/`LaunchedEffect(destination)`.
  - `ScreenTracker` usa `distinctUntilChanged` por `NavBackStackEntry.id` (evita duplicar em rotação/recomposição) e roda no main sem alocar listas (mapa pré-calculado uma vez com `remember`).
  - Sem `Dispatchers.IO` extra; sem coleta de flow nova nas ViewModels, exceto `UserPropertiesSync` (1 coleta combinada, `distinctUntilChanged`, escopo do app).
  - Volume: `ui_action` só para controles relevantes (sem digitação nem cada tecla); scroll só por faixa de profundidade, uma vez por tela.
- **Feature toggle:**

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `analytics_collection_enabled` | true | DataStore local (`SettingsRepository`), toggle em Configurações > Dados  | `setAnalyticsCollectionEnabled(false)`, para de enviar e limpa user properties/`user_id` | coleta completa |
  | `feat_analytics_ui_actions` | true | remote-config (kill-switch, opcional) | `ui_action` não é enviado, demais eventos seguem | envia |

- **Observabilidade:** nomes canônicos em `AnalyticsEvents`/`AnalyticsScreen`/`AnalyticsElements`; proibido `trackEvent("literal")` (teste Konsist).
- **Compatibilidade:** minSdk 28 sem impacto; nada de Firestore; sem migração. Histórico do GA continua válido, pois nomes de evento e `screen_name` existentes não mudam (só `screen_class` muda de valor, o que é o objetivo).
- **Privacidade/LGPD:** `user_id` apenas com o consentimento ativo; `docs/` (política e `index.html`) e `strings.xml` (pt-BR e en-US) atualizados com o que passa a ser coletado.

## 7. Plano de Testes
- **Unitários:**
  - `FirebaseAnalyticsTrackerTest`: sanitização (nome inválido, prefixo reservado, >25 params, valor >100 chars truncado), `Boolean`, `null`, falha do SDK vai ao `CrashReporter`.
  - `ScreenTrackerTest`: rota mapeada dispara `(id, screenClass)`; rota não mapeada não dispara (e registra warning); mesma entrada não duplica; dialog dispara.
  - Uma suíte por ViewModel alterada (Cart, ShoppingList, EditShopping, ProductForm, ProductPhoto, Settings*, Auth*, Paywall, Profile): evento esperado, parâmetros esperados, **ausência** de PII; os testes que hoje verificam `trackScreenView("x", "XViewModel")` (ex.: `CartViewModelTest.kt:105`, `ShoppingListViewModelTest.kt:110`) passam a garantir que a ViewModel **não** chama `trackScreenView`.
  - `AiChatViewModelTest`: sucesso (sent → received com latência > 0), falha de rede (`IOException` → `error_type=network`), falha genérica, voz não suportada, toggle OFF.
  - `BaseAgentTest`: tool executada com sucesso, com exceção, bloqueada por Pro, ação inexistente.
  - `FallbackAgentTest`: fallback emite `ai_provider_fallback`; relançar (tool já executada) não emite.
  - `UserPropertiesSyncTest`: muda tema/idioma/plano → `setUserProperty`; logout → limpa; consentimento OFF → nada enviado.
- **Arquitetura (Konsist, em `ArchitectureTest.kt`):**
  1. Nenhum `trackScreenView` em classes `*ViewModel`.
  2. Nenhuma chamada `trackEvent("...")` com literal fora de `core:analytics`.
  3. Todo `NavKey` serializável de `composable<>`/`dialog<>` aparece em algum `screenNames`.
  4. Valores de `AnalyticsEvents` únicos, snake_case, ≤ 40 chars.
- **Instrumentados / UI:** `testTag` já usados nos fluxos existentes; adicionar um teste em `:app` que navega Welcome → lista → carrinho e verifica, com `AnalyticsTracker` fake via Hilt test module, a sequência de telas. Sem asserts de texto (app roda em pt-BR e en).
- **Manual:** DebugView (`adb shell setprop debug.firebase.analytics.app br.com.brunocarvalhs.howmuch`) cobrindo o roteiro das seções 6.
- **Comandos:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`. Sem `@Suppress` (regra do projeto; o `TooGenericExceptionCaught` de `FirebaseAnalyticsTracker` hoje está em `core/analytics/config/detekt/baseline.xml`, não deve crescer).

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Coletar mais dados sem o opt-out prometido em `docs/index.html:136` | alta (já ocorre) | alto (LGPD, política da Play) | P1: toggle real antes de ampliar eventos; atualizar textos legais |
| Estourar limites do GA4 (500 eventos, 25 params, 50 dimensões) | média | médio | `ui_action` genérico + catálogo fechado; sanitizer; registrar só dimensões necessárias |
| PII em parâmetro (nome de produto, texto da IA, `e.message`) | média | alto | parâmetros só de enum/bucket/contagem; testes de "ausência de PII"; revisão de maintainer |
| Duplicar `screen_view` durante a migração | média | baixo | remover os 16 `trackScreenView` no mesmo PR do `ScreenTracker`; teste Konsist 1 |
| Rota nova esquecida no mapa | média | baixo | teste Konsist 3 falha o build |
| Mudar `screen_class` quebra Explorations que filtram pela ViewModel | baixa | médio | `screen_name` não muda; avisar no CHANGELOG; ajustar filtros no GA (memória: Explorations do projeto Cestou) |
| `FeatureNavGraph` ganhar membro afeta `:wear` | baixa | baixo | default `emptyMap()`; `registerWearGraph` intacto |
| Volume de `ui_action` por clique | baixa | baixo | só elementos do catálogo, sem debounce necessário por serem cliques discretos; kill-switch remoto |

## 9. Perguntas em Aberto (bloqueantes)
- [x] **P1 - Opt-out de Analytics:** toggle INCLUÍDO no escopo (respondida em 2026-11-02). `setAnalyticsCollectionEnabled` em Configurações > Dados, só Analytics. Com ele OFF: nenhum evento, `user_id` e user properties limpos.

Respondidas pelo usuário (2026-11-02):
- **P2 - Identificação:** `user_id` = UID do Firebase Auth (conta Google ou e-mail/senha), usado só para identificação interna e para localizar o cliente no console. Enviado apenas com a coleta ligada e limpo no logout ou ao desligar o toggle. **E-mail NÃO vai para o GA** (ver decisão D1). Pago x não pago via user property `plan`.
- **P3 - Cliques:** cobertura máxima ("cada comportamento"). Todo controle interativo relevante emite `ui_action`, com `element_id` no formato `<tela>.<elemento>` vindo de constantes (nada de string solta). Cliques sem valor (ex.: fechar teclado) ficam de fora por não serem "comportamento".
- **MainActivity:** é Activity única, o `screen_view` automático dela não mede nada. Será desligado (`google_analytics_automatic_screen_reporting_enabled=false`); só o `ScreenTracker` reporta telas.

### Decisões derivadas
- **D1 - E-mail:** os Termos do Google Analytics proíbem enviar PII (e-mail, nome, telefone) em eventos, propriedades ou `user_id`. Risco: suspensão da propriedade e exclusão de dados. Alternativa adotada: `user_id` = UID (pseudônimo, mapeável para o e-mail em Firebase Console > Authentication). Se quiser ver e-mail junto de métricas, o caminho é BigQuery export + join com a lista de Auth, fora do GA.
- **D2 - Plano pago/gratuito:** user property `plan` (`free`/`pro`) atualizada por `UserPropertiesSync` a partir de `SubscriptionRepository.observeStatus()` (mesma fonte do gate Pro em `BaseAgent.kt:isPro`), mais `plan_source` (`play_billing`). Registrar `plan` como dimensão de usuário no GA4 para filtrar qualquer relatório por pago x gratuito. Evento `plan_changed` (de, para) em toda transição.
- **D3 - Mais comportamentos cobertos** (além da tabela da seção 4), todos sem PII:
  - Ciclo de vida: `app_foreground`, `app_background` (duração da sessão em segundos), `app_cold_start` (tempo até a 1ª tela), `deep_link_opened` (tipo), `invite_link_received`.
  - Navegação: `tab_selected` (bottom bar), `back_pressed`, `dialog_dismissed`, `sheet_opened`, `sheet_dismissed`, cada um com evento próprio e os parâmetros já registrados (`screen_name` implícito, `element_id`, `method` = gesto/botão/sistema). Decisão do usuário em 2026-11-02: revelam abandono e hesitação (ex.: voltar do paywall, dispensar o dialog de finalizar compra). Não custam dimensão nova, pois reutilizam parâmetros existentes.
  - Lista/produto: `product_add_started`, `product_add_abandoned`, `product_price_edited`, `product_note_edited`, `product_sorted` (modo), `product_list_scrolled_end` (profundidade em faixas), `list_empty_state_viewed`, `quick_add_used`, `recipe_used`.
  - Compra: `budget_exceeded_viewed`, `budget_warning_shown`, `purchase_cancelled`.
  - Colaboração: `member_added`, `member_removed`, `qr_code_generated`, `scanner_permission_result`.
  - Sistema: `permission_requested`/`permission_result` (câmera, microfone, notificações), `notification_received`, `notification_opened`, `network_offline_detected`, `sync_failed`.
  - Conta: `account_deleted`, `data_cleared`, `language_changed` (já existe), `theme_changed`, `rate_app_clicked`, `support_opened` (tipo), `legal_viewed` (termos/privacidade).
  - Assinatura: `paywall_viewed`, `plan_selected`, `purchase_started`, `purchase_succeeded`, `purchase_failed` (código), `purchase_restored`, `subscription_manage_opened`.
  - Erros: `operation_failed` em todo `catch` de ViewModel que hoje só mostra mensagem, e `ai_*_failed`.
- **D3b - Tracks adicionais (valor de negócio e produto):**
  - **Ativação e retenção:** `first_list_created`, `first_product_added` (com `seconds_since_signup`), `first_purchase_completed`, `session_count_bucket` (user property recalculada), `returned_after_days` (1, 3, 7, 14, 30 ao voltar), `streak_weeks_active`.
  - **Descoberta de recursos:** `feature_first_used` (param `feature`: ai_chat, photo_scan, voice, qr_share, history, insights, recipes, quick_add, wear), disparado uma única vez por usuário (flag em DataStore). Responde "quais recursos ninguém achou".
  - **Funil de conversão:** `paywall_trigger_hit` (qual limite ou recurso Pro bloqueou: `cart_gate`, `ai_gate`, `insights`), `paywall_viewed`, `plan_selected`, `purchase_started`, `purchase_succeeded`, `purchase_failed`, `paywall_dismissed` (tempo na tela em segundos), `upgrade_banner_clicked`.
  - **Valor percebido (sem valores monetários):** `purchase_completed` com `items_count_bucket`, `budget_used_pct_bucket` (0-50, 51-90, 91-100, >100), `list_completion_pct_bucket`, `duration_minutes_bucket` (tempo entre criar e finalizar).
  - **Qualidade da IA:** `ai_message_regenerated`, `ai_message_copied`, `ai_suggestion_accepted`/`ai_suggestion_rejected` (quando houver confirmação de ação), `ai_conversation_length_bucket` ao fechar o chat, `ai_empty_response`.
  - **Foto e scan:** `photo_scan_started`, `photo_scan_ocr_result` (`on_device`, `gemini_fallback`, `failed`), `photo_scan_items_found_bucket`, `photo_scan_items_confirmed_bucket`. Complementa os eventos já existentes.
  - **Busca e catálogo:** `search_no_results` (só `query_length`), `search_result_selected` (posição), `suggestion_used`.
  - **Colaboração:** `invite_sent` (canal), `invite_accepted`, `list_shared_member_count_bucket`.
  - **Notificações:** `notification_received`, `notification_opened`, `notification_dismissed`, `notification_permission_prompted/result`, `reminder_scheduled`.
  - **Saúde técnica:** `app_crash_recovered` (reinício após crash detectado), `slow_screen_load` (> 2 s até a 1ª composição útil, com `screen_name`), `api_error` (`operation`, `http_class` 4xx/5xx/timeout), `offline_action_queued`.
  - **Sinais de saída:** `account_deleted`, `sign_out`, `app_uninstall_hint` (inatividade ≥ 14 dias detectada no próximo retorno).
  - **Avaliação:** `rate_prompt_shown`, `rate_clicked`, `feedback_submitted` (categoria, nunca o texto).
- **Dimensões do GA4 a registrar** (limite 50 de evento e 25 de usuário; manter ≤ 20 em uso): evento: `element_id`, `entity`, `source`, `feature`, `error_type`, `operation`, `provider`, `action_id`, `input_method`, `trigger`; usuário: `plan`, `ever_pro`, `ai_trial_used`, `account_age_bucket`, `is_tester`, `app_language`, `has_wear`.
- **Regra para novos eventos:** todo evento tem KDoc de uma linha dizendo que comportamento do cliente ele revela. O limite real a vigiar são as dimensões personalizadas (parâmetros novos), não o número de nomes de evento: reusar parâmetros já registrados antes de criar um novo.
- **D4 - Enforcement do "tudo":** teste Konsist exige que todo campo lambda de classe `*Intent` (padrão MVI, ex.: `AiChatIntent.kt`, `CartIntent`) seja coberto: ou a ViewModel chama `trackAction`/`trackMutation` ao montar o lambda, ou o campo vem na lista explícita de isenções (navegação pura de voltar etc.), que só pode encolher.

## Status de implementação (2026-11-03)
Registro do que foi entregue e do que foi adiado por decisão do usuário após a auditoria da D3/D3b. Em caso de divergência com as seções acima, vale esta.

- **Entregue:** `screen_view` central (`ScreenTracker`, `screenNames` em todas as features), catálogo de eventos/telas/elementos, `ui_action`/`trackMutation`/`trackError`, `AnalyticsSanitizer`, user properties e `user_id` (UID) via `UserPropertiesSync`, opt-out em Configurações > Dados (aplicado só pelo `analyticsEnabled`), telemetria da IA (`ai_response_received` com `provider`, `tool_calls`, `fallback_used`; `ai_tool_executed`; `ai_provider_fallback` com `from`; voz; sessões), `app_function_invoked`, ciclo de vida e retenção, ativação (`first_*`, `feature_first_used`), conta/assinatura básica, e os eventos de hesitação `back_pressed`, `tab_selected`, `paywall_dismissed` e `product_add_abandoned`. Kill-switch remoto `feat_analytics_ui_actions`.
- **Adiado (fora desta entrega, sem constante no catálogo):** `product_add_started` (coberto por `screen_view` do seletor), `search_result_selected`, `suggestion_used`, `member_added/removed`, `qr_code_generated`, `scanner_permission_result`, `invite_*`, `permission_*`, `notification_received/dismissed`, `reminder_scheduled`, `network_offline_detected`, `sync_failed`, `api_error`, `slow_screen_load`, `app_crash_recovered`, `offline_action_queued`, `app_uninstall_hint`, `plan_selected`/`purchase_succeeded`/`purchase_restored`/`paywall_trigger_hit` (o app tem um plano só, `subscription_started` já cobre a compra e `paywall_viewed` traz o gatilho em `source`), `ai_message_regenerated/copied`, `ai_suggestion_*`, `ai_empty_response`, `photo_scan_items_*`, `rate_*`, `feedback_submitted` (ver sdd-034). Entram quando houver a funcionalidade ou a pergunta de negócio que os justifique.
- **Removidos do catálogo por não terem onde emitir:** `sheet_opened`, `sheet_dismissed` (os sheets são destinos de navegação: `screen_view` + `back_pressed`), `product_sorted`, `search_no_results`. `product_search_performed`, `product_selected` e `search_mode` são nomes antigos mantidos sem emissor, porque a busca de produto só existe como ferramenta da IA e como `AppFunction`.
- **Teste de sequência de telas (§7):** feito como teste Robolectric com `NavHost` real (`ScreenTrackingNavigationTest`), não instrumentado com Hilt falso: o app não tem a infraestrutura de Hilt para testes. Cobre o mapeamento real rota -> `screen_name` e o `back_pressed`.
- **Versão:** `versionName` permanece `2.2.1` até o release de produção; o bump (2.3.0) acontece só nessa etapa.
- **Definições personalizadas no GA4 (2026-11-03):** registradas na propriedade `how-much-2a72e`. Evento (15): `element_id`, `entity`, `source`, `feature`, `error_type`, `operation`, `provider`, `action_id`, `input_method`, `trigger`, `score`, `kind`, `value`, `reason`, `tool_calls`. Usuário (8): `plan`, `ever_pro`, `ai_trial_used`, `account_age_bucket`, `is_tester`, `app_language`, `has_wear`, `nps_class`. Métrica (1): `latency_ms` (milissegundos). Propriedades de usuário extras registradas: `lists_bucket`, `currency`, `theme`, `ai_provider`, `notifications_enabled`, `reminders_enabled`, `sorting_mode` (total 30 definições). Roteiro manual de telas validado em 2026-11-03 num SM-A146M com o build debug da branch (via logcat do Firebase): `screen_name` canônico, `screen_class` terminando em `Screen`, nenhum `ViewModel`/`MainActivity`, voltar gera novo `screen_view` e `back_pressed`, `tab_selected` e `user_id` = UID. Não exercitado: os eventos do sdd-034 (exigem finalizar compra e votar na IA) e `has_wear` (sem relógio pareado).

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (e `architecture/` onde existe; lacuna registrada).
- [x] Nenhum código de produção escrito (apenas assinaturas/diagramas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Impacto público declarado como `additive` (nenhuma assinatura existente muda; `FeatureNavGraph` ganha membro com default).
- [x] Novo comportamento remoto/coleta: toggle definido (`analytics_collection_enabled`, kill-switch `feat_analytics_ui_actions`) e observabilidade planejada.
- [x] Sem novo cache; `UserPropertiesSync` mantém só o último valor enviado em memória.
- [x] Perguntas em aberto exauridas.
