# Task: Funcionalidades secundárias no relógio (IA e notificações)

- **Slug:** wear-secondary-features
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-10
- **Status:** ready
- **Versão-alvo:** a definir pelo processo de release (ver Q5)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive — novas rotas internas ao `feature:shopping` e novo contrato de relay IA entre relógio e celular (`WearablePaths` e `WearableSyncService` em `core:common`).

## 1. Contexto e Motivação

- A etapa 1 (já implementada) pôs um ícone de menu na home do relógio que abre `WearMenu` com Histórico e Configurações. A etapa 2 leva ao relógio as funcionalidades secundárias que hoje só existem no celular: **IA** e **notificações**.
- Análise de gastos já existe no relógio como Histórico: `feature/shopping/.../presentation/wear/screen/WearHistoryScreen.kt` (rota `WearHistory`, `navigation/wear/WearInternalRoutes.kt`). Não entra neste plano.
- **IA no celular:** entrada é o `dialog<AiChat>` em `feature/chat/.../navigation/ChatGraph.kt:34`, com `AiChatScreen` e `AiTurnService` (`feature/chat/.../data/service/AiTurnService.kt:19`). O motor é `feature:ai-agent` (`GeminiAgent.kt:21-22` lê `BuildConfig.GEMINI_API_KEY`). A chave BYOK fica nas configurações do celular (`feature/settings/.../AiTier3Content.kt:18`).
- **Notificações no celular:** sino com badge entregue pelo sdd-028 (commit `da53eb3d`), chamado em `feature/shopping/.../presentation/screen/ShoppingScreen.kt:123`. Coleção Firestore `notifications` via `NotificationRepository` (`core:domain`) e `NotificationRepositoryImpl` (`core:data`). Lembrete diário local: `feature/settings/.../data/worker/ShoppingReminderScheduler.kt:23,45` e `ShoppingReminderWorker.kt:97`.
- **Relógio hoje:**
  - Lê o Firestore direto: `feature/shopping/.../presentation/wear/viewmodel/ShoppingDetailViewModel.kt:69` usa `shoppingRepository.observeById` com listeners.
  - Lê o resumo local das listas do `WearListStore` (`feature/shopping/.../data/local/WearListStore.kt:27`) para a tile e a home.
  - Recebe o login do celular no pareamento (`core/common/.../wearable/WearablePaths.kt:23`, `AUTH_PAIR`).
  - Já depende de `:feature:chat`, `:feature:ai-agent` e `:feature:settings` (`wear/build.gradle.kts:87-89`), mas **nenhuma rota de IA ou de notificações está registrada no relógio** (`feature/shopping/.../navigation/wear/ShoppingWearGraph.kt`).
- Rastreabilidade do menu da etapa 1: `WearMenuScreen.kt` (criada), `ShoppingWearGraph.kt` (`composable(WearMenu::class.java.name)`).

## 2. Escopo

- **Inclui:**
  - **Notificações no relógio (Q2):** tela `WearNotifications` com lista da coleção `notifications`, badge de não lidas e ação "Marcar lidas". Não abre a lista ao tocar.
  - **IA no relógio (Q1a, Q4):** entrada rápida. O usuário dita ou digita uma frase curta, o celular roda o agente (`feature:ai-agent`) com a própria chave, e o relógio mostra a resposta curta. Ações de escrita pedem confirmação no relógio antes de executar.
  - Entrada no `WearMenu` para cada item novo (`WearNotifications`, e a rota de IA).
  - Testes unitários dos ViewModels e do relay, e testes de UI com `testTag`.
- **Exclui:**
  - Mudanças no celular além do listener de relay (sino, `AiChatScreen`, configurações, lembrete).
  - Chat completo no relógio (Q1b descartada).
  - Análise de gastos (já coberta pelo Histórico).
  - Lembrete diário no relógio (Q3: fica só no celular).
  - Push FCM no relógio.
  - Sincronizar a chave de IA (Q4: a chave nunca sai do celular).
  - Migração do schema de `notifications` (já coberto pelo sdd-028).

## 3. Impacto Arquitetural

- **Módulos/camadas:**
  - `:feature:shopping` — `presentation/wear` (telas, ViewModels, estado, intents), `navigation/wear` (rotas), `domain` (reuso dos casos de uso de notificação).
  - `:core:common` — `WearablePaths` e `WearableSyncService` (contrato do relay IA).
  - `:core:data` — implementação do relay no `WearableSyncServiceImpl`.
  - `:feature:ai-agent` / `:feature:chat` (celular) — só o listener que recebe o prompt, chama o agente e devolve a resposta. Nenhuma tela nova no celular.
- **Padrão:** Clean Arch + MVI, rotas como string (`WearMenu::class.java.name`), como as demais rotas Wear. ViewModels do relógio com Hilt (`hiltViewModel()`).
- **Novos arquivos (base `feature/shopping/src/main/java/.../feature/shopping/`):**
  - `presentation/wear/screen/WearNotificationsScreen.kt`
  - `presentation/wear/viewmodel/WearNotificationsViewModel.kt`
  - `presentation/wear/state/WearNotificationsUiState.kt`
  - `presentation/wear/intent/WearNotificationsIntent.kt`
  - `presentation/wear/screen/WearAiScreen.kt`
  - `presentation/wear/viewmodel/WearAiViewModel.kt`
  - `presentation/wear/state/WearAiUiState.kt`
  - `presentation/wear/intent/WearAiIntent.kt`
  - `navigation/wear/WearInternalRoutes.kt` (novas `internal data object WearNotifications` e `WearAi`)
- **Diagrama (IA via celular):**

```
Relógio                                              Celular
WearAiScreen (ditado/digitação)
   │ intent Send(prompt)
WearAiViewModel ──► WearableSyncService.sendAiPrompt(nodeId, requestId, prompt)
                          │  MessageClient  /ai/prompt
                          ▼
                                              WearAiListener (novo, :core:data)
                                                 │ AgentOrchestrator (feature:ai-agent)
                                                 │ usa a chave do próprio celular
                          ▲  MessageClient  /ai/reply
WearableSyncService.observeAiReply(requestId) ◄──┘
   │ Flow<WearAiMessage.Reply>
WearAiViewModel ──► estado Answer / Confirm / Error
```

- **Diagrama (notificações, relógio):**

```
WearMenuScreen ──onNavigateToNotifications──► ShoppingWearGraph
                                                   │
                                  WearNotificationsScreen
                                                   │ intent
                                  WearNotificationsViewModel (Hilt)
                                     │                    │
                     ObserveUnreadNotificationsCountUseCase   MarkAllNotificationsReadUseCase
                                     │
                       NotificationRepository (core:domain) ── Firestore `notifications`
```

- **Injeção:** Hilt no `di/` do módulo `feature:shopping` (relógio) e no `di/` de `core:data` (relay). Nenhum repositório instanciado em Composable (AGENTS §5).

## 4. Contratos e Interfaces

- **Rotas internas (`internal`, sem contrato público):**
  - `internal data object WearMenu` (já criada na etapa 1).
  - `internal data object WearNotifications` (nova).
  - `internal data object WearAi` (nova).
- **Casos de uso reutilizados (sem mudança de assinatura):** `ObserveUnreadNotificationsCountUseCase`, `MarkAllNotificationsReadUseCase`, `GroupNotificationsUseCase` (`feature/shopping/.../domain/usecase/`).
- **Contrato do relay IA (público entre módulos, `core:common`):**
  - `WearablePaths`: novos `AI_PROMPT = "/ai/prompt"` e `AI_REPLY = "/ai/reply"`, com codec de `requestId` e texto (mesmo padrão de `encodeWearConfirm`/`encodeAuthPair`).
  - `WearableSyncService` (interface, `core/common/.../wearable/WearableSyncService.kt`):
    ```
    suspend fun sendAiPrompt(phoneNodeId: String, requestId: String, prompt: String): Boolean
    fun observeAiReply(requestId: String): Flow<WearAiMessage.Reply>
    ```
  - `WearAiMessage.Reply(requestId, text, requiresConfirmation, confirmationId)`: sealed em `core/common/.../wearable/WearAiMessage.kt`.
  - **Justificativa de `public`:** o relógio (`wear`) e o celular (`app`/`core:data`) são módulos diferentes e compartilham o contrato, então não pode ser `internal`. Exige teste unitário do codec e registro no CHANGELOG pelo processo de release (AGENTS §6).
- **Escrita com confirmação:** a resposta da IA pode trazer uma ação de escrita (`requiresConfirmation = true`). O relógio mostra "Confirmar" ou "Cancelar". Só o `confirmationId` confirmado volta ao celular (`AI_PROMPT` com `confirm=true`). Nada é executado sem essa confirmação.
- **Nada novo no Firestore** (usa a coleção `notifications` existente).

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Notificações só aparecem no celular, no sino com badge | Relógio mostra a mesma coleção, com a mesma contagem de não lidas | `ShoppingScreen.kt:123`; sdd-028 §2 |
| 2 | Lembrete diário é notificação local do sistema, fora da coleção | Continua só no celular (Q3) | `ShoppingReminderScheduler.kt:45`; `ShoppingReminderWorker.kt:97` |
| 3 | IA só roda no celular, com a chave do celular | Continua no celular. O relógio envia o prompt e recebe a resposta, sem chave | `AiTier3Content.kt:18`; `GeminiAgent.kt:21-22` |
| 4 | Análise de gastos é o Histórico do relógio | Sem mudança | `WearHistoryScreen.kt` |
| 5 | Ações de IA alteram dados (tools) | Escrita só depois de confirmação no relógio | sdd-017, sdd-032 |

## 6. Critérios de Aceitação

- Performance:
  - Notificações: um listener do Firestore com `isRead == false` (uma query). Nenhum I/O em `@Composable`.
  - IA: uma requisição por vez por relógio. Timeout explícito no relay (sem espera infinita por resposta do celular).
  - Recomposições: estado imutável, coleções via `StableList`, como em `ShoppingWearScreen.kt`.
  - Coroutines: o relay usa `Dispatchers.IO` (`Wearable.getMessageClient`) e cancela o `observeAiReply` quando a tela sai.
- Feature toggles:

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `wear_ai_quick_entry` | false | remote-config | entrada de IA oculta no `WearMenu` | entrada visível |

  Notificações não têm toggle: é leitura de dados que já existem.
- Observabilidade: eventos de Analytics em `core:analytics` (`AnalyticsElements.kt`): `wear_menu_open`, `wear_notifications_open`, `wear_ai_prompt_sent`, `wear_ai_action_confirmed`. Não logar texto do prompt, conteúdo de notificação nem token.
- Compatibilidade: Wear OS (minSdk 28), sem migração de dados locais. O relógio já tem o login do celular (`AUTH_PAIR`).
- Segurança: a chave de IA não sai do celular. Prompts e respostas não são persistidos no relógio.

## 7. Plano de Testes

- **Unitários:**
  - `WearNotificationsViewModel`: com não lidas, zero não lidas, erro do Firestore, marcar todas como lidas.
  - `WearAiViewModel`: resposta simples, resposta que pede confirmação, confirmação, cancelamento, celular indisponível (`sendAiPrompt` false), timeout de resposta.
  - Codec de `WearablePaths` para `AI_PROMPT`/`AI_REPLY`, com entrada inválida.
  - Relay no celular: prompt sem chave configurada devolve erro claro, não cai.
  - Flag `wear_ai_quick_entry` OFF oculta a entrada no `WearMenu`.
- **Instrumentados / UI (Robolectric, como `ShoppingWearScreensTest.kt` e `WearMenuScreenTest.kt`):** cenários de `WearNotificationsScreen` e `WearAiScreen`, com `testTag` (sem asserts de texto).
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`.
- **Manual (obrigatório antes do PR):** relógio pareado com celular ao lado, e relógio sem celular por perto (o estado "celular não encontrado" tem que aparecer).

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Chat completo não cabe na tela de 1,4" | alta | alto | Escopo limitado a entrada rápida (Q1a) |
| IA depende do celular estar por perto e com o app em execução | alta | médio | Estado "celular não encontrado" (como `openPhoneApp`); timeout no relay |
| Listener do celular que recebe `/ai/prompt` precisa estar registrado e rodando | média | alto | Verificar o listener de mensagens existente no celular antes de implementar; se não houver, o item entra no escopo |
| Ação de escrita da IA executada sem confirmação | baixa | alto | Confirmação obrigatória no relógio (regra 5), com teste |
| Vazamento de chave de IA | baixa | alto | A chave não sai do celular (Q4); teste de que o payload não contém chave |
| Duplicar lembrete (celular e relógio) | baixa | médio | Q3: lembrete só no celular |
| Contrato público novo (`WearableSyncService`) sem revisão | média | médio | Revisão de um maintainer antes da implementação (AGENTS §8, passo 3) |

## 9. Decisões

- **Q1 (IA, escopo): entrada rápida (a).** Uma frase curta, ditada ou digitada, com resposta curta. Chat completo (b) e resumo (c) ficam fora. Ações de escrita pedem confirmação no relógio.
- **Q2 (notificações): só leitura e "marcar lidas".** Tocar no aviso não abre a lista no relógio.
- **Q3 (lembrete diário): só no celular.** Não há agendamento no relógio e não há sincronização de configuração.
- **Q4 (chave de IA): a chave fica no celular.** O relógio não recebe chave e não sincroniza configuração de IA. A IA roda no celular, e o relógio só troca mensagens com ele (ver §3 e §4). Esta é a decisão concreta da recomendação, que antes estava vaga.
- **Q5 (versão): a definir pelo processo de release.** Este plano não toca em `CHANGELOG.md` nem em `versionName`, conforme `AGENTS.md` e a regra do projeto para PRs de feature. A divergência entre `AGENTS.md` (2.2.1) e o git (`prepare 2.3.2`) fica para o maintainer.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha`.
- [x] Nenhum código de produção escrito (apenas assinaturas e diagramas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `additive` em "Impacto público", com justificativa de `public` em §4.
- [x] Novo remote-config: toggle `wear_ai_quick_entry` definido, observabilidade planejada.
- [x] Cache: nenhum cache novo proposto (Firestore com cache offline dos listeners; IA sem persistência no relógio).
- [x] Perguntas em aberto exauridas (Q1 a Q5 decididas).
- [ ] Revisão de um maintainer antes da implementação (AGENTS §8, passo 3).

## 11. Desvios registrados na implementação (revisão)

Registrados após a implementação, para manter spec e código coerentes. A seção 3 e a seção 4 acima continuam valendo como intenção original, com estas diferenças:

- **Relay IA no módulo `app`**, não em `core:data`: `core:data` não pode depender de `feature:ai-agent`. Arquivos: `app/src/main/java/br/com/brunocarvalhs/howmuch/wear/WearAiRelay.kt` e `WearAiListenerService.kt`.
- **Contrato dividido:** os métodos de IA ficam em `WearableAiLink` (`core/common/.../wearable/WearableAiLink.kt`), não em `WearableSyncService`, para respeitar o limite de funções do Detekt. A implementação é `WearableAiLinkImpl` (`core/data`).
- **Mensagens:** `WearAiMessage` (sealed: `Reply`, `ConfirmRequest`, `Failure`) substitui `WearAiMessage.Reply` isolado. `observeAiMessages(requestId)` substitui `observeAiReply`.
- **Caminhos:** `/ai/prompt`, `/ai/reply`, `/ai/confirm-request`, `/ai/confirm` e `/ai/failure`. Codec em `WearAiCodec` e `WearAiPayload.kt`.
- **Confirmação:** reaproveita a aprovação do agente (`AiSession.pendingApproval` e `resolveApproval`), não um campo `confirm=true` com `confirmationId`.
- **Limite do prompt:** 300 caracteres (`WEAR_AI_MAX_PROMPT_LENGTH`), para não estourar a Data Layer.
- **Contagem de não lidas:** `NotificationRepository.observeUnreadCount` (leitura só dos não lidos). A lista ainda lê a coleção inteira, mostrando no máximo 30 itens.
- **Sessão do agente:** o relay limpa o histórico (`orchestrator.clearHistory()`) antes de cada prompt do relógio. Isso afeta o chat do celular se os dois forem usados ao mesmo tempo.

### Decisão tomada: confirmação de escritas no relógio (opção a)

- **Regra:** no relógio, toda ação de escrita pede aprovação do usuário antes de executar. As escritas são `WRITE_ACTION_IDS` (`core/ai/.../model/WriteConfirmation.kt`): `add_common_products_to_cart`, `add_multiple_products_to_cart`, `add_recipe_ingredients`, `clear_purchased_products`, `create_shopping_list`, `delete_multi_shopping_list`, `delete_product`, `delete_shopping_list`, `reopen_shopping_list`, `save_product_to_cart` e `update_shopping_list`. Leituras (`get_all_shopping_lists`, `get_shopping_list_details`, `search_products`, `search_recipes`, `open_shopping_by_id`) não pedem aprovação.
- **Como:** o relay envia `REQUIRE_WRITE_CONFIRMATION_KEY = true` no `toMetadata()` da requisição do relógio. `requiresApproval` (`core/ai/.../model/WriteConfirmation.kt`, usada por `BaseAgent.kt`) junta essa chave com o `requiresConfirmation` que já existia.
- **Chat do celular:** não envia a chave. A regra dele não muda, e ações que já pediam aprovação continuam pedindo em qualquer lugar.
- **Testes:** `WriteConfirmationTest` (core:ai) e o teste do metadata em `WearAiRelayTest` (app).
