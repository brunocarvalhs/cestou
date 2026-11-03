# Task: Dar ao agente de IA acesso seguro a todas as funcionalidades do app (criar, ler, editar, excluir)

- **Slug:** ai-agent-acesso-total-app
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-02
- **Status:** blocked (2 decisões de produto em §9; Fase 0 pode começar antes de elas serem respondidas)
- **Versão-alvo:** 2.4.0 (minor — capacidade nova e visível ao usuário)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive — novos membros com default em `core:ai` (`AgentAction.risk`, `AgentAction.toggleKey`, `@AiAgentAction.risk`) e um helper em `core:domain`. Nenhuma assinatura existente muda. Ver §4 e AGENTS.md §6.

## 1. Contexto e Motivação

- Pedido (usuário, via `/code-planner`): "um plano onde as features que o app tem a IA tenha acesso, modificar, editar, excluir e todas as funcionalidades do app fácil para o app".
- Hoje o agente tem **15 tools**, todas sobre **listas** e **adição de produtos** (8 em `feature:shopping`, 7 em `feature:products`). Ele não consegue editar nem excluir um produto, marcar como comprado, mexer em orçamento/favorito/emoji, consultar histórico ou gastos, alterar configurações, nem abrir uma tela.
- "Fácil para o app" tem dois sentidos aqui: (a) o usuário resolve tudo pelo chat, e (b) o time adiciona uma capacidade nova de IA com um arquivo e uma linha de DI, sem esquecer nada. O plano cobre os dois.
- **Alargar o acesso aumenta o risco.** O desenho abaixo parte de que dar poder de escrita e exclusão a um modelo gratuito fraco só é aceitável com confirmação forçada no código (não no prompt), checagem de papel e kill switch.

### Rastreabilidade (estado atual, verificado no código em HEAD)

| # | Achado | Evidência |
|---|---|---|
| F1 | 15 tools registradas via `@IntoSet` | `ShoppingAgentModule.kt:22-52` (8), `ProductAgentModule.kt` (7), `AgentRegistry.kt:15-23` |
| F2 | O modelo **não consegue editar/excluir um produto**: não há tool, e `get_shopping_list_details` devolve produtos **sem `id`** | `ShoppingGetDetailsUseCase.kt:46-55` (só `name/quantity/price/isPurchased/category`); `ProductRepository.kt` já tem `updateProduct`/`deleteProduct` |
| F3 | **Bug de chave de metadata:** o contexto expõe `"shopping_id"`, mas `ProductSaveUseCase` e `ShoppingGetDetailsUseCase` leem `"shopping"`. Hoje só funciona porque o prompt manda o modelo repassar `shoppingId` | `AiChatUiState.kt:19-22` vs `ProductSaveUseCase.kt:65` e `ShoppingGetDetailsUseCase.kt:33` |
| F4 | Nome de parâmetro inconsistente: `shopping_id` (6 tools) vs `shoppingId` (4 tools) | `ShoppingUpdateUseCase.kt:44`, `ProductSaveUseCase.kt:64` |
| F5 | `delete_shopping_list` **não valida o papel**, mas `delete_multi_shopping_list` valida OWNER. Têm a mesma descrição ("Deleta uma lista…"), o que confunde o modelo | `ShoppingDeleteUseCase.kt:26-29` vs `ShoppingMultiDeleteUseCase.kt:36-43`; `ShoppingMultiDeleteUseCase` só é usado via DI (busca de usos), já `ShoppingDeleteUseCase` também é usado pela UI (`ShoppingListViewModel.kt:50`) |
| F6 | `open_shopping_by_id` só **devolve dados**, não navega. O nome promete uma coisa que a tool não faz | `ShoppingGetByIdUseCase.kt:13-35` |
| F7 | O prompt manda usar categorias padrão, mas `save_product_to_cart` não tem parâmetro `category`: todo produto criado pela IA fica "Outros" | `SystemPrompts.kt:16`; `ProductSaveUseCase.kt:68-73` (`Product(...)` sem `category`) |
| F8 | **Não existe confirmação** antes de ação destrutiva. `delete_*` e `clear_purchased_products` executam direto no chokepoint | `BaseAgent.kt:45-80` |
| F9 | `requiresPro` existe e nenhuma tool o usa (decisão comercial pendente) | `AgentAction.kt:18`, grep: só `BaseAgent.kt:51` |
| F10 | Já houve tool criada e **não registrada** (gap de DI) — corrigido em `7f2a6f0a`. Nada impede de acontecer de novo | `git log`: `fix/agent-registry-di-gap` |
| F11 | Papéis existentes: só `OWNER` e `EDITOR` (não há `VIEWER`) | `User.kt:7-10` |
| F12 | O escopo do prompt é estrito ("só listas, carrinho, produtos, preços e receitas") e recusaria configurações/gastos | `SystemPrompts.kt:23` (regra 8) |
| F13 | Custo por request: cada tool vira uma declaração enviada a **todo** request; modelos gratuitos já travam em loop (teto de 5 rodadas) | `OpenRouterMapping.kt:30-50`, `AiAgentLimits.kt:8` |

## 2. Escopo

- **Inclui:**
  - **Fase 0 — Fundamentos de segurança e consistência** (`core:ai`, `core:domain`, `feature:ai-agent`): classificação de risco por ação, gate de confirmação no chokepoint, helper de papel, correção de F3/F4/F5/F7, kill switch por remote-config, teste Konsist de cobertura.
  - **Fase 1 — CRUD completo de produtos** (`feature:products`): editar, excluir, marcar comprado em lote; `category` na criação; `id` no detalhe da lista.
  - **Fase 2 — Listas completas** (`feature:shopping`): orçamento, favorito, emoji, estabelecimento, duplicar, entrar por código, sair, obter código de compartilhamento; exclusão consolidada.
  - **Fase 3 — Leitura inteligente**: histórico e resumo de gastos (depende de `sdd-029`/`sdd-031`).
  - **Fase 4 — Configurações e navegação**: `get_app_settings`/`update_app_settings` (allowlist) e `open_screen`.
  - Atualização do system prompt e do catálogo de capacidades.
- **Exclui (de propósito):**
  - Excluir conta, apagar todos os dados e limpar cache. Ações de conta irreversíveis ficam só na UI (ver Q2).
  - Mudar `aiProvider`, `aiModel`, `aiConfigTier`, `creativityLevel` e **principalmente `customApiKey`**: o agente não reconfigura a si mesmo nem toca em segredo.
  - Comprar/cancelar assinatura (`feature:subscription`, `core:billing`).
  - Gate Pro nas tools novas (decisão comercial; ver Q-não-bloqueante 2).
  - Desfazer ("undo") por soft-delete no Firestore. É a alternativa mais forte à confirmação, mas é mudança de modelo de dados (ver Riscos).
  - Wear OS e `feature:auth`/`feature:profile`.

## 3. Impacto Arquitetural

- **Camadas/módulos afetados**

| Módulo | Camada | Mudança |
|---|---|---|
| `core:ai` | contract/annotation/utils | `ActionRisk`, `AgentAction.risk`, `AgentAction.toggleKey`, `@AiAgentAction(risk)`, constante de metadata, helper `resolveShoppingId` |
| `core:domain` | model | extensões `Shopping.requireMember/requireOwner` |
| `feature:ai-agent` | data/service | `ConfirmationGate`, integração em `BaseAgent.executeAction`, filtro por toggle, system prompt |
| `feature:shopping` | domain/di | 6 tools novas, `update_shopping_list` estendida, consolidação das exclusões |
| `feature:products` | domain/di | `update_product`, `delete_products`, `set_products_purchased`, `category`, tools de produtos comuns |
| `feature:settings` | domain/di (**nova** `implementation(project(":core:ai"))`) | `get_app_settings`, `update_app_settings`, `SettingsAgentModule` |
| `feature:chat` | presentation | só a chave de metadata (F3) e texto de confirmação, sem mudança de fluxo |
| `core:navigation` | — | sem mudança; `open_screen` reusa o `Navigator` singleton (`Navigator.kt`) |

- **Padrões:** Clean Architecture + MVI (sem mudança de UI). Cada capacidade nova = 1 classe `AgentActionUseCase` + 1 `@Binds @IntoSet` no `*AgentModule` da própria feature (padrão de `sdd-027`).

- **Novos arquivos (caminhos completos, raiz `…/howmuch/`)**
  - `core/ai/src/main/java/br/com/brunocarvalhs/howmuch/core/ai/contract/ActionRisk.kt`
  - `core/domain/src/main/java/br/com/brunocarvalhs/howmuch/core/domain/model/ShoppingAccess.kt`
  - `feature/ai-agent/.../data/service/ConfirmationGate.kt`
  - `feature/shopping/.../domain/usecase/ShoppingDuplicateAgentUseCase.kt`, `ShoppingJoinAgentUseCase.kt`, `ShoppingLeaveUseCase.kt`, `ShoppingShareCodeUseCase.kt`
  - `feature/products/.../domain/usecase/ProductUpdateUseCase.kt`, `ProductsDeleteUseCase.kt`, `ProductsSetPurchasedUseCase.kt`, `CommonProductAddUseCase.kt`, `CommonProductRemoveUseCase.kt`
  - `feature/settings/.../domain/usecase/GetAppSettingsAgentUseCase.kt`, `UpdateAppSettingsAgentUseCase.kt`, `feature/settings/.../di/SettingsAgentModule.kt`
  - `feature/shopping/.../domain/usecase/ShoppingHistoryAgentUseCase.kt`, `SpendingSummaryAgentUseCase.kt` (Fase 3, reaproveitam `GroupHistoryByMonthUseCase` do `sdd-029`)
  - `feature/shopping/.../domain/usecase/OpenScreenUseCase.kt`
  - `app/src/test/.../architecture/AgentCapabilityTest.kt` (Konsist)
  - `architecture/06-capacidades-do-agente.md` (catálogo: tool, risco, papel exigido, toggle)

- **Fluxo (chokepoint único)**

```
usuário ─▶ AiChatViewModel ─▶ CartAssistantUseCase ─▶ Agent.sendMessage (novo turno)
                                                        │  ConfirmationGate.onNewTurn()
                                                        ▼
                       modelo pede tool(args) ─▶ BaseAgent.executeAction
                                                        │
              ┌─────────── registry.find(id) ──────────┤
              │  toggle OFF? ───────────────▶ "Recurso indisponível"
              │  requiresPro && !pro? ──────▶ "Exclusivo do plano Pro"
              │  risk == DESTRUCTIVE?
              │     ├─ sem pendência igual de turno anterior ─▶ registra pendência
              │     │      └▶ "AGUARDANDO_CONFIRMACAO: pergunte ao usuário…"  (não executa)
              │     └─ pendência igual do turno anterior ─────▶ executa
              ▼
      action.execute(args, session, metadata)
              └─▶ use case: resolveShoppingId() → repo.getById → requireMember/requireOwner
                       └▶ repositório (Firestore) ─▶ toToolFeedback() ─▶ modelo
```

- **Hilt:** `ConfirmationGate` é `@Singleton` com `@Inject constructor`, injetado em `AgentDependencies` (mesmo caminho de `crashReporter`/`registry`). Tools novas entram só por `@Binds @IntoSet`. Nada de service locator.

## 4. Contratos e Interfaces

> Assinaturas apenas, sem implementação.

**`core:ai` — `public`, aditivo.** Justificativa de `public`: `AgentAction` já é o contrato entre `core:ai` e as features (AGENTS.md §6); o chokepoint em `feature:ai-agent` precisa ler `risk`/`toggleKey` sem conhecer cada feature.

```kotlin
public enum class ActionRisk { READ, WRITE, DESTRUCTIVE }

public interface AgentAction<R> {
    // existentes: id, description, parameters, requiresPro, execute(...)
    public val risk: ActionRisk get() = ActionRisk.WRITE
    /** Chave de remote-config que desliga esta ação sem release; `null` = sempre ligada. */
    public val toggleKey: String? get() = null
}

@AiAgentAction(id, description, risk: ActionRisk = ActionRisk.WRITE, toggleKey: String = "")   // annotation: novo campo com default
public object AgentMetadata { public const val SHOPPING_ID: String = "shopping_id" }
public fun Map<String, Any?>.resolveShoppingId(metadata: Map<String, Any?>): String?
    // aceita "shoppingId" e "shopping_id" nos args; cai para metadata[SHOPPING_ID]
```

Default `WRITE` (não `READ`) para que uma tool esquecida nunca seja tratada como leitura. As leituras existentes recebem `risk = READ` explícito.

**`core:domain` — `public` (usado por `shopping`, `products`, `settings`, que não se enxergam entre si; AD-011 já usa `core:domain` para isso).**

```kotlin
public fun Shopping.requireMember(userId: String?)   // lança ListAccessDeniedException se userId ∉ users
public fun Shopping.requireOwner(userId: String?)    // lança OwnershipRequiredException-equivalente se roles[userId] != OWNER
```

Elimina a checagem inline duplicada em `ShoppingUpdateUseCase.kt:63-66` e `ShoppingMultiDeleteUseCase.kt:36-43`. Usa `authService.currentUser?.id` **na hora da execução**, nunca `AiSession.userId` (congelado na criação do singleton, `AgentModule.kt:43-48`; conclusão do `sdd-017`).

**`feature:ai-agent` — `internal`.**

```kotlin
internal class ConfirmationGate @Inject constructor() {
    fun onNewTurn()                                              // incrementa turno; expira pendência > 1 turno
    fun check(actionId: String, args: Map<String, Any?>): Decision // Pending(msg) | Confirmed
    fun reset()                                                  // chamado em AiSession.clearHistory()/logout
}
```

A pendência é chaveada por `actionId + args canonizados` (chaves ordenadas, valores normalizados via `getString`). **Só confirma em turno posterior ao da pendência**; um modelo que repete a chamada no mesmo turno não se auto-confirma. Sem mudança em `AiSession`, `Agent` ou `AgentContext`.

**Tools (final: 27 = 15 − 2 removidas + 14 novas)**

| Tool | Módulo | Risco | Papel exigido | Fase |
|---|---|---|---|---|
| `get_all_shopping_lists`, `get_shopping_list_details` (**+`id` por produto**), `search_products`, `search_recipes` | shopping/products | READ | membro | 0 |
| `create_shopping_list`, `add_recipe_ingredients`, `add_multiple_products_to_cart`, `add_common_products_to_cart`, `reopen_shopping_list` | existentes | WRITE | membro | 0 |
| `save_product_to_cart` (**+`category`**) | products | WRITE | membro | 0/1 |
| `update_shopping_list` (**+`budget`, `isFavorite`, `emoji`, `establishment`**) | shopping | WRITE; `users`/`roles` só OWNER (já existe) | membro | 2 |
| `delete_shopping_list` (**absorve** `delete_multi_shopping_list`) | shopping | **DESTRUCTIVE** | **OWNER** | 0 |
| `clear_purchased_products` | products | **DESTRUCTIVE** | membro | 0 |
| `update_product` (`productId`; `name`, `quantity`, `price`, `category`, `isPurchased`) | products | WRITE | membro | 1 |
| `delete_products` (`productIds: list`) | products | **DESTRUCTIVE** | membro | 1 |
| `set_products_purchased` (`productIds` ou `all=true`, `purchased: bool`) | products | WRITE | membro | 1 |
| `add_common_product` / `remove_common_product` | products | WRITE / WRITE | usuário logado | 4 |
| `duplicate_shopping_list` | shopping | WRITE | membro | 2 |
| `join_shopping_list` (`shortCode`) | shopping | WRITE | — | 2 |
| `leave_shopping_list` | shopping | **DESTRUCTIVE** | membro | 2 |
| `get_share_code` | shopping | READ | membro | 2 |
| `get_shopping_history`, `get_spending_summary` | shopping | READ | membro | 3 |
| `get_app_settings` (nunca retorna `customApiKey`) | settings | READ | — | 4 |
| `update_app_settings` (**allowlist**: `themeMode`, `language`, `currency`, `notificationsEnabled`, `reminderTime`, `sortingMode`, `defaultListId`, `remindersEnabled`) | settings | WRITE, `toggleKey = feat_ai_agent_settings_tools` | — | 4 |
| `open_screen` (`target`: enum de rotas permitidas, `shoppingId?`) | shopping | READ, `toggleKey = feat_ai_agent_navigation` | — | 4 |

- **Removidas:** `delete_multi_shopping_list` (duplicata, F5) e `open_shopping_by_id` (substituída por `open_screen`, F6).
- **Parâmetros:** o nome canônico passa a ser `shoppingId`; o helper `resolveShoppingId` ainda aceita `shopping_id` (F4), então não há quebra com histórico de conversa em andamento. `AiAgentParameter.type` já é repassado ao JSON Schema (`OpenRouterMapping.kt:48`); usar `"boolean"`/`"number"` onde couber (hoje `quantity` e `price` caem no default `"string"`, `ProductSaveUseCase.kt:24-25`).
- **Sem NavKey nova.** `open_screen` usa rotas existentes de `core:navigation` (`NavRoutes.kt`) e a de `sdd-029`/`sdd-031` quando existirem.
- **Remote-config (novos):** `feat_ai_agent_destructive_tools`, `feat_ai_agent_settings_tools`, `feat_ai_agent_navigation`. **Firestore:** nenhuma coleção nova. **Analytics:** ver §6.
- **BREAKING:** nenhum. Os nomes de tool removidos só existem no contrato modelo↔app e não são persistidos (histórico de chat guarda texto, não chamadas de tool; confirmar em `ChatMessageModel.kt` na implementação).

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | Ação destrutiva executa na primeira chamada | Só executa se o **mesmo** pedido for repetido num turno posterior ao pedido de confirmação | `BaseAgent.kt:45-80` |
| 2 | `delete_shopping_list` não checa papel | Exige OWNER, no mesmo código usado pela UI e pelo agente (evita o padrão A-02 de regra divergente entre `invoke` e `execute`, `sdd-025`) | `ShoppingDeleteUseCase.kt:26-29`, `ShoppingListViewModel.kt:50` |
| 3 | `delete_multi_shopping_list` valida OWNER | Removida; comportamento absorvido por `delete_shopping_list` | `ShoppingMultiDeleteUseCase.kt:36-43` |
| 4 | Só OWNER altera `users`/`roles` | Mantido, via `requireOwner` | `ShoppingUpdateUseCase.kt:60-69` |
| 5 | Qualquer ação de escrita num item não valida que o chamador é membro | Toda tool que recebe `shoppingId` chama `requireMember` | novo (hoje confiado às regras do Firestore, que, segundo `architecture/05` AD-009, ainda **não foram publicadas**) |
| 6 | Produto criado pela IA sempre `category = "Outros"` | Parâmetro `category` normalizado via `ProductCategory.fromString` (`core:ui`); valor inválido → "Outros" | `ProductSaveUseCase.kt:68-73`, `SystemPrompts.kt:16` |
| 7 | Edição de produto grava histórico | `update_product` acrescenta `ProductActivity.Action.EDITED` (ou `PURCHASED` quando só `isPurchased` muda) com o `userId` do chamador | `Product.kt:15-19`, `ProductSaveUseCase.kt:102-103` |
| 8 | Agente nunca age com identidade própria | Mantido: `userId` vem de `authService.currentUser`, não de `AiSession` | `ProductSaveUseCase.kt:75-78` |
| 9 | `shopping_id` do contexto é ignorado por 2 tools | `resolveShoppingId` usa a chave única `AgentMetadata.SHOPPING_ID` | `AiChatUiState.kt:19-22` |
| 10 | Prompt restringe a "listas, carrinho, produtos, preços, receitas" | Escopo ampliado a configurações do app, histórico e gastos; recusa de conteúdo adulto/fora do app **mantida** (regras 9-10) | `SystemPrompts.kt:23-25` |
| 11 | Lista tem orçamento e prompt manda avisar ao estourar | `update_shopping_list(budget)` aceita `null`/0 para remover; ao estourar, o resultado da tool inclui o aviso | `SystemPrompts.kt:18`, `Shopping.kt:19` |
| 12 | `update_app_settings` não existe | Só campos da allowlist; qualquer outro campo → falha explícita ("campo não permitido"), nunca ignorado em silêncio | novo |

## 6. Critérios de Aceitação

- **Segurança**
  - Nenhuma tool `DESTRUCTIVE` executa sem pendência prévia de turno anterior (teste de unidade cobre: mesmo turno, turno seguinte, args diferentes, pendência expirada, `reset`).
  - Um membro que não é OWNER não consegue excluir a lista nem alterar `users`/`roles`, tanto pelo chat quanto pela UI.
  - `customApiKey` nunca aparece em retorno de tool, log, Crashlytics ou Analytics.
  - O teste Konsist falha se alguma subclasse de `AgentActionUseCase` não tiver `@AiAgentAction`, não estiver em um `@IntoSet`, ou for `DESTRUCTIVE` sem checagem de papel (heurística: usa `requireOwner`/`requireMember`).
- **Performance**
  - Sem I/O duplicado: `delete_products`, `set_products_purchased` e `clear_purchased_products` lêem os produtos **uma vez** (`getAllProducts(...).first()`) e escrevem em lote; os 2 últimos hoje fazem loop de escrita individual (`ShoppingClearPurchasedUseCaseImpl.kt:30-32`), aceitável, mas limitar a 50 itens por chamada e retornar quantos foram feitos.
  - Orçamento de contexto: as declarações de tool somam **≤ 27**; medir o tamanho do JSON de declarações antes/depois e registrar no PR. Se passar de +60% sobre o baseline, agrupar por toggle antes de seguir.
  - Escrita Firestore fora da main (`Dispatchers.IO` já usado nos repositórios); `open_screen` navega em `Dispatchers.Main.immediate` e sai com aviso se `Navigator.navControllerOrNull == null` (`Navigator.kt` `navigate()` já tolera).
  - `ConfirmationGate` guarda no máximo 1 pendência (sem crescimento); sem alocação por request fora dos args canonizados.
- **Feature toggles**

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `feat_ai_agent_destructive_tools` | true | remote-config | tools `DESTRUCTIVE` somem das declarações e respondem "indisponível" se chamadas | disponíveis com confirmação |
  | `feat_ai_agent_settings_tools` | true | remote-config | `get/update_app_settings` ocultas | disponíveis |
  | `feat_ai_agent_navigation` | true | remote-config | `open_screen` oculta | disponível |

  Leitura do toggle feita **uma vez por `sendMessage`** (não por tool call), com fallback para o default se o remote-config falhar.
- **Observabilidade** (`core:analytics`; `feature:ai-agent` hoje não depende dele, `build.gradle.kts:17-20`, adicionar):
  - `ai_action_executed` {`action_id`, `risk`, `outcome`: success|failure|denied|toggle_off}
  - `ai_confirmation` {`action_id`, `stage`: requested|confirmed|expired}
  - Sem args, sem nomes de lista, sem IDs de usuário (AGENTS.md §5).
  - Crashlytics: `crashReporter.recordException` já recebe `function` nos extras (`BaseAgent.kt:66-72`); acrescentar `risk`.
- **Compatibilidade:** minSdk 28; Wear OS não é afetado (módulo standalone, sem `core:ai`); sem migração de dados locais/Firestore. Mudar `language` pelo chat recria a Activity e o `Navigator` religa (`Navigator.unbind` já cobre a corrida); validar que o chat não perde a conversa nesse caso (risco abaixo).
- **i18n (AGENTS.md §5):** textos que o usuário vê (ex. erro de permissão exibido no chat) vão em `strings.xml` pt-BR/en/es. As strings do system prompt e as respostas das tools continuam literais pt-BR, **dívida já conhecida** (F-07, `AiAgentLimits.kt:12-17`); não ampliar essa dívida além do necessário.

## 7. Plano de Testes

- **Unitários (JUnit + coroutines test)**
  - `ConfirmationGateTest`: mesmo turno não confirma; turno N+1 confirma; args em ordem diferente canonizam igual; args diferentes → nova pendência; N+2 expira; `reset()` limpa.
  - `BaseAgent` chokepoint: `READ`/`WRITE` passam direto; `DESTRUCTIVE` devolve `AGUARDANDO_CONFIRMACAO` e **não** chama `execute`; toggle OFF; `requiresPro`; exceção de use case continua virando texto (`AgentFactoryImplTest`/`GeminiAgentGatingTest` como modelo).
  - `ShoppingAccessTest`: membro, não-membro, OWNER, EDITOR, `userId` nulo.
  - Por tool nova: caminho feliz, argumento ausente, lista inexistente, chamador não-membro, falha do repositório (`Result.failure`). Em `update_product`: só `isPurchased` gera `PURCHASED`; mudança de preço gera `EDITED`. Em `update_app_settings`: campo fora da allowlist falha; `customApiKey` e `aiProvider` rejeitados; idioma/moeda inválidos rejeitados.
  - Regressão F3: `ProductSaveUseCase` e `ShoppingGetDetailsUseCase` resolvem lista a partir de `metadata["shopping_id"]`.
  - Regressão F5: `delete_shopping_list` por EDITOR falha; por OWNER executa (após confirmação).
  - `get_shopping_list_details` devolve `id` por produto.
- **Arquitetura (Konsist, `AgentCapabilityTest`)**: ver §6.
- **Instrumentados/UI:** sem UI nova. Se houver fluxo Maestro de chat, seletores por `testTag`, nunca por texto (app roda em pt-BR e en). Cenário manual em dispositivo: "apaga a lista" → pede confirmação → "sim" → exclui; "apaga a lista" → "não" → nada acontece.
- **Lint/Detekt/Cobertura:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout` (cobertura ≥ 85%). **Sem `@Suppress`** (AGENTS.md §5): se uma tool nova passar de `TooManyFunctions`/`LongMethod`, extrair helper.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Modelo gratuito alucina um `productId` e edita/exclui o item errado | média | alto | `productId` só vale se existir na lista (`require`); `delete_products` é DESTRUCTIVE (confirmação); resultado da tool devolve os **nomes** afetados para o modelo reportar |
| Usuário responde "sim" a uma pergunta diferente da pendente | baixa | médio | a pendência é por args canonizados: se o modelo repetir com outros args, abre nova pendência em vez de executar |
| 27 tools estouram o contexto/aderência de modelos gratuitos (F13) | média | médio | meta ≤ 27 com tools consolidadas (`update_product` única, `set_products_purchased` em lote); medir tamanho; toggle por grupo; teto de 5 rodadas já existe |
| Firestore rules não publicadas (AD-009) tiram a 2ª camada de defesa | alta | alto | checagem de papel no app (`requireMember/Owner`) + confirmação; tratar `D-01` em paralelo |
| `delete_shopping_list` com guard muda comportamento da UI para não-OWNER | média | médio | confirmar em `ShoppingListViewModel.kt` o que a UI mostra a não-OWNER (provavelmente "sair"); alinhar para as duas rotas usarem a mesma função |
| Trocar `language` pelo chat recria a Activity no meio do turno | média | baixo | aplicar o idioma por último no turno e validar que o histórico do chat sobrevive; se não, tirar `language` da allowlist |
| Pendência de confirmação vazar entre contas | baixa | alto | `reset()` em logout e em `clearHistory()` (mesmos pontos do `sdd-006` B-11) |
| `sdd-029`/`sdd-031` atrasam e bloqueiam a Fase 3 | média | baixo | Fase 3 é independente das Fases 0-2 e 4; entregar sem ela |
| Concorrência: duas tools de escrita no mesmo turno sobre o mesmo produto | baixa | baixo | escritas Firestore parciais por campo; sem cache local de produto em memória no agente |
| Cache | — | — | Nenhum cache novo. Remote-config já tem cache próprio; `ConfirmationGate` é estado em memória do turno, sem persistência (justificativa: expira em 1 turno e morre com o processo, que é o comportamento desejado) |

Ordem de entrega recomendada (um PR por fase, cada um com lint/detekt/testes verdes):
**0** → **1** → **2** → **4** → **3**. A Fase 0 corrige F3, F4, F5, F7, F8 e F10 e tem valor próprio mesmo se o resto for adiado.

## 9. Perguntas em Aberto (bloqueantes)

- [ ] **Q1 — Política de confirmação.** Recomendo: confirmar sempre `delete_shopping_list`, `leave_shopping_list`, `clear_purchased_products` e `delete_products` (qualquer quantidade). A alternativa é confirmar `delete_products` só acima de N itens (precisa de risco dinâmico, mais complexo). Posso seguir com a recomendação?
- [ ] **Q2 — Ações de conta pelo chat.** Recomendo manter **fora** (excluir conta, apagar todos os dados, limpar cache) e **sem** `customApiKey`/provedor de IA. Se você quiser algumas delas pelo chat, preciso saber quais, porque exigem confirmação reforçada (digitar uma palavra) e não são cobertas por este desenho.

Perguntas **não bloqueantes** (sigo com o default se não houver resposta):
- Qual a posição do `delete_products` quanto a "desfazer"? Default: sem undo nesta versão (soft-delete fica para um `sdd` próprio).
- Alguma tool nova deve ser Pro (`requiresPro`)? Default: nenhuma (F9, decisão comercial pendente).
- Local dos use cases de histórico/gastos do `sdd-029`/`sdd-031`: se ficarem só em `feature:shopping`, as tools da Fase 3 também ficam lá; se `feature:products` precisar delas, expor porta em `core:domain` (padrão AD-011).
- `architecture/` não documenta o agente de IA nem as tools (só AD-010 cita o gate Pro). Lacuna registrada; o arquivo `architecture/06-capacidades-do-agente.md` desta tarefa a preenche.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (e `architecture/` onde existe: AD-009, AD-010, AD-011).
- [x] Nenhum código de produção escrito (apenas assinaturas e pseudocódigo).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `additive` em "Impacto público" (§4 justifica cada `public`).
- [x] Novo endpoint/remote-config: toggles definidos (3), observabilidade planejada.
- [x] Novo cache: nenhum; estado do gate justificado (memória, 1 turno).
- [ ] Perguntas em aberto exauridas (Q1 e Q2 pendentes, por isso `status: blocked`).
- [ ] Ao implementar: atualizar `AGENTS.md` §2 se `feature:settings` passar a depender de `core:ai`; `CHANGELOG.md` via release; `fastlane/metadata` (feature visível); `.agents/skills/android-architecture/SKILL.md` (nova convenção de `risk`/`toggleKey`).
