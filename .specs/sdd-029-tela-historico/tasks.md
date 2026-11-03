# Task: Tela de Histórico de compras (aba na barra inferior)

- **Slug:** tela-historico
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-02
- **Status:** ready
- **Versão-alvo:** 2.3.0 (minor — feature visível; mesmo release de `sdd-028`)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive — `Shopping` (`core:domain`) ganha `finishedAt: Long? = null` (se a pergunta Q1 for confirmada); nova rota `History : NavKey` em `core:navigation`; coleção Firestore `shopping` ganha 1 campo opcional.

## 1. Contexto e Motivação

- Pedido: "criar uma tela de histórico com o objetivo de lista" (artboard **"4 · Histórico (cards por mês com gráfico)"** do canvas Cestou, aprovado; fluxo em **F1 · bloco 4 HISTÓRICO**).
- Hoje **não existe** tela de histórico nem aba para ela. A barra inferior tem só Listas e Perfil (`core/ui/.../components/CestouBottomNavigation.kt:26-42,57-60`); compras finalizadas aparecem apenas misturadas às demais em Listas, via filtro `FINISHED` (`ShoppingListViewModel.kt:245`).
- O dado necessário já existe: uma lista com `status == FINISH` carrega `price` e `establishment` gravados no fim da compra (`FinishPurchaseViewModel.kt:57-66`). **Falta a data da finalização**: `Shopping` só tem `createdAt` (`Shopping.kt:15`), e a data de criação da lista não é a data da compra (ver Q1).
- Design (descrição do que a tela mostra):
  - Título "Histórico"; carrossel horizontal de **cards de mês** (SET/OUT/NOV, 112×132) com o total do mês no rodapé do card e uma **linha de tendência** ligando os pontos; mês selecionado destacado (laranja, borda 2px); mês atual selecionado por padrão.
  - Seção "Compras de {mês}" com **cards-lista**: dia/mês abreviado à esquerda, nome do estabelecimento, subtítulo "{título da lista} · N pessoas", valor à direita.
  - Aba "Histórico" ativa na barra inferior (Listas · Histórico · Perfil).
- F1 define o alvo do toque: "Lista de compras do mês selecionado (ao deslizar os cards)" e "Detalhe da compra (**a definir**)" — ver Q2.
- Rastreabilidade: `Shopping.kt:8-22`, `ShoppingModel.kt:8-45`, `ShoppingRepository.kt` (`observeAll`), `ShoppingRepositoryImpl.kt:40-61`, `MainActivity.kt:~170-221` (rootRoutes + bottomBar), `ShoppingGraph.kt:55-67`, `MobileRoutes.kt:15-18`, `firestore.rules:86-160`, `architecture/05-decisoes-arquiteturais.md` (AD-009: rules de `shopping`).

## 2. Escopo

- **Inclui:**
  - Nova aba **Histórico** na `CestouBottomNavigation` (ícone de barras, label `nav_history`), entre Listas e Perfil.
  - Rota `History` (`NavKey`, `PROTECTED`) registrada no `shoppingGraph` e em `rootRoutes` de `MainActivity`.
  - Tela `HistoryScreen` (MVI): carrossel de meses com total + linha de tendência, lista de compras do mês selecionado, estados loading / vazio / erro.
  - Agrupamento por mês a partir de `observeAll()` filtrado por `FINISH`, em use case puro e testável (relógio injetável).
  - Toque em uma compra abre `CartFlow(shopping)` (decisão provisória, Q2).
  - Gravar `finishedAt` nos 3 caminhos que finalizam uma lista e limpá-lo ao reabrir (se Q1 = sim).
  - Eventos de Analytics (`history_viewed`, `history_month_selected`, `history_purchase_opened`).
  - Strings em `values`, `values-pt-rBR`, `values-es` de `feature:shopping` (e `nav_history` nos 3 de `core:ui`).
  - `testTag` em todos os elementos testáveis.
- **Exclui:**
  - Tela de detalhe própria da compra (Q2) e filtros/busca/exportação.
  - Aba/telas de Histórico no Wear OS (artboards S1/S2) — plano separado.
  - Paginação/limite de histórico: `observeAll()` já traz todas as listas do usuário; volume por usuário é pequeno (ver `project_auth_user_base_small`); reavaliar se virar problema.
  - Backfill de `finishedAt` em listas antigas (usam fallback `createdAt`, ver §5 regra 2).
  - Gráfico além dos cards (sem biblioteca de charts; a linha é desenhada em `Canvas`).

## 3. Impacto Arquitetural

- **Módulos/camadas:**
  - `:core:domain` — `domain` (`Shopping.finishedAt`).
  - `:core:navigation` — `History` em `mobile/MobileRoutes.kt`.
  - `:core:ui` — `BottomNavItem.History` + string `nav_history`.
  - `:feature:shopping` — `domain` (use cases + modelos), `data` (modelo/mapper), `presentation`, `navigation`, strings, `di` (nada novo se os use cases forem `@Inject constructor`).
  - `:feature:cart` — `FinishPurchaseViewModel` grava `finishedAt`.
  - `:app` — `MainActivity` (`rootRoutes`).
  - Raiz — `firestore.rules` (sem mudança obrigatória, ver §8), `CHANGELOG.md` (via release).
- **Decisão de módulo:** dentro de `feature:shopping`, **não** um `feature:history`. Motivos: a tela só consome `ShoppingRepository` (já em `core:domain`) e navega a `CartFlow` por `core:navigation`; `sdd-028` seguiu o mesmo caminho para Notificações; um módulo novo exigiria `settings.gradle`, `FeatureNavGraph`/`FeatureInitializer`, config Kover/Detekt e entrada na tabela de `AGENTS.md` §2 por uma tela de lista. O `ArchitectureTest` (Konsist) só aceita as camadas `domain/data/presentation/navigation/di` — os arquivos abaixo respeitam isso.
- **Padrão:** Clean Architecture + MVI (intent como data class, estado imutável com `ImmutableList`/`StableList`), Nav3 tipada.
- **Novos arquivos** (base `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/`):
  - `domain/model/HistoryModels.kt` (`HistoryMonth`, `HistoryPurchase`)
  - `domain/usecase/ObserveHistoryUseCase.kt` (observa `observeAll()`, filtra `FINISH`, delega agrupamento)
  - `domain/usecase/GroupHistoryByMonthUseCase.kt` (puro: agrupa, soma, ordena, `Clock`/`ZoneId` injetáveis)
  - `presentation/state/HistoryUiState.kt`, `presentation/intent/HistoryIntent.kt`
  - `presentation/viewmodel/HistoryViewModel.kt`
  - `presentation/screen/HistoryScreen.kt` (+ `HistoryContent` sem ViewModel, para teste/preview)
  - `presentation/components/history/HistoryMonthCard.kt`, `HistoryMonthTrend.kt` (carrossel + linha em `Canvas`), `HistoryPurchaseRow.kt`, `HistoryEmptyState.kt`
  - Testes espelhando cada item (§7).
- **Arquivos alterados:** `Shopping.kt`, `ShoppingModel.kt`, `ShoppingMapper.kt`, `ShoppingReopenUseCase.kt`, `ShoppingUpdateUseCase.kt`, `ShoppingListViewModel.kt` (caminho `ShoppingAction.Update`), `FinishPurchaseViewModel.kt`, `MobileRoutes.kt`, `CestouBottomNavigation.kt`, `MainActivity.kt`, `ShoppingGraph.kt`, `AnalyticsEvents.kt`, `strings.xml` (x3 em `feature:shopping`, x3 em `core:ui`).

```
 BottomNav(Histórico) ──navigate(History)──► composable<History> (ShoppingGraph)
                                                   │ hiltViewModel
                                                   ▼
                                           HistoryViewModel ──intent──► (selectMonth, openPurchase, retry)
                                                   │ uiState: StateFlow<HistoryUiState>
                                                   ▼
                              ObserveHistoryUseCase ─► ShoppingRepository.observeAll() (core:domain)
                                                   │ Flow<List<Shopping>> (status == FINISH)
                                                   ▼
                              GroupHistoryByMonthUseCase  (puro, Clock injetável)
                                                   ▼
                              List<HistoryMonth>{ key, total, purchases }

 toque numa compra ──► navigator.navigate(CartFlow(shopping))
```

- **Hilt:** use cases e ViewModel com `@Inject constructor`; `Clock` já provido? Verificar `GroupNotificationsUseCase` (`sdd-028`) e reutilizar o mesmo binding/padrão; se não houver, fornecer `Clock` em `ShoppingModule.kt` (`di/`).
- **Threading:** agrupamento roda no `Dispatchers.Default` (`flowOn`) — não bloqueia main; coleta com `collectAsStateWithLifecycle`.

## 4. Contratos e Interfaces

(Assinaturas apenas.)

- **`Shopping` (`core:domain`, `public`)** — **additive**: `val finishedAt: Long? = null`. `public` porque o modelo é contrato entre `core:data`/features/Wear; consumidores: `feature:shopping`, `feature:cart`, `feature:ai-agent` (via `ShoppingUpdateUseCase`), `wear`, testes. Valor default `null` mantém construtores existentes compilando e docs antigos legíveis.
- **`ShoppingModel` (`internal`)** — `val finishedAt: Long? = null`; `toMap()` inclui `"finishedAt" to finishedAt`; mapper bidirecional atualizado.
- **`History : NavKey, RouteProtocol` (`core:navigation`, `public`)** — `data object History`, `routeType = PROTECTED`. `public` porque `core:ui` (barra) e `app` (`rootRoutes`) a referenciam, igual a `Profile` (`MobileRoutes.kt:15-18`).
- **`BottomNavItem.History` (`core:ui`, `public`)** — `route = History`, `titleRes = R.string.nav_history`, ícone barras (vetor próprio, sem nova dependência de ícones estendidos).
- **Domain (`internal`)**:
  - `data class HistoryPurchase(val shopping: Shopping, val date: Long, val establishment: String?, val peopleCount: Int, val total: Double)` — `date = finishedAt ?: createdAt`; `peopleCount = users.size.coerceAtLeast(1)`.
  - `data class HistoryMonth(val key: YearMonth, val total: Double, val purchases: List<HistoryPurchase>)`.
  - `class GroupHistoryByMonthUseCase @Inject constructor(clock: Clock) { operator fun invoke(lists: List<Shopping>): List<HistoryMonth> }` — meses em ordem **crescente** (esquerda → direita no carrossel), compras em ordem **decrescente** por `date`.
  - `class ObserveHistoryUseCase @Inject constructor(repository: ShoppingRepository, group: GroupHistoryByMonthUseCase) { operator fun invoke(): Flow<List<HistoryMonth>> }`.
- **MVI (`internal`)**:
  - `data class HistoryUiState(val isLoading: Boolean = true, val months: StableList<HistoryMonth> = emptyStable, val selectedMonth: YearMonth? = null, val error: UiText? = null)` + derivados `selectedPurchases`, `isEmpty`.
  - `interface HistoryIntent { fun onMonthSelected(key: YearMonth); fun onPurchaseClick(purchase: HistoryPurchase); fun onRetry() }` (mesmo padrão de `NotificationsIntent.kt`).
- **Analytics (`core:analytics`, constantes em `AnalyticsEvents.kt`)**: `HISTORY_VIEWED = "history_viewed"`, `HISTORY_MONTH_SELECTED = "history_month_selected"`, `HISTORY_PURCHASE_OPENED = "history_purchase_opened"`; reutilizar `AnalyticsParams.SHOPPING_ID`; novo param `MONTH` (`yyyy-MM`) só se não existir.
- **Firestore:** coleção `shopping` ganha o campo opcional `finishedAt` (Number). Sem nova coleção/índice: a query continua `whereArrayContains("users", uid)` (`ShoppingRepositoryImpl.kt:~47-52`) e o filtro/ordenação são client-side.
- **Remote-config:** nenhum (ver §6, toggle).

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Compra finalizada = `status == FINISH`, com `price` e `establishment` gravados pelo fluxo "Finalizar compra" | Histórico lista **somente** `FINISH`; total do mês = soma de `price` dessas listas | `FinishPurchaseViewModel.kt:57-66`, `ShoppingListViewModel.kt:245` |
| 2 | Só existe `createdAt`; "mês da compra" hoje seria o mês de criação | `date = finishedAt ?: createdAt`. Listas antigas e sem `finishedAt` caem no mês de criação (limitação aceita, Q1) | `Shopping.kt:15`, `ShoppingListViewModel.kt:~256` (`toMonthYearString`) |
| 3 | Há **3 caminhos** que põem a lista em `FINISH`: (a) sheet "Finalizar compra", (b) quick-finish na linha da lista, (c) agente de IA (`update_shopping_list`) | Os três passam a gravar `finishedAt = now` quando o status resultante é `FINISH`; sem isso, (b) e (c) cairiam no fallback e a data ficaria errada | `FinishPurchaseViewModel.kt:57-66`; `ShoppingListViewModel.kt:216-224`; `ShoppingUpdateUseCase.kt:55-59` |
| 4 | Reabrir move para `IN_PROGRESS` e mantém `price` | Reabrir limpa `finishedAt` (`FieldOp.Delete`) para a compra sair do mês antigo até ser finalizada de novo | `ShoppingReopenUseCase.kt:~22-26`, `ShoppingRepository.kt` (doc de `updateFields`) |
| 5 | `price` pode ser `0.0` em lista finalizada sem valor | Compra com `price <= 0` **aparece** na lista do mês (é uma compra), mas soma 0; mês com total 0 mostra "R$ 0,00" | `Shopping.kt:12` |
| 6 | `establishment` é opcional | Linha exibe `establishment ?: título da lista` como título e `description`/pessoas como subtítulo (sem texto literal; vazio tratado) | `Shopping.kt:21` |
| 7 | `users.size > 1` identifica lista em grupo | Subtítulo "N pessoa(s)" usa plural com `users.size` (mín. 1) | `ShoppingListViewModel.kt:244` |
| 8 | Barra inferior some em rotas fora de `rootRoutes` | `History` entra em `rootRoutes`, logo a barra aparece nela e continua oculta em Carrinho/detalhes (conforme F1) | `MainActivity.kt:~170-176,209-215` |
| 9 | Rules: membro pode atualizar campos de conteúdo (b) | Gravar `finishedAt` por membro é permitido (a regra (b) só pina `users`, `roles`, `shortCode`) — sem mudança de rules | `firestore.rules:~135-143` |

## 6. Critérios de Aceitação

- **Funcional:**
  - Aba "Histórico" visível e selecionável; estado preservado ao alternar abas (`saveState`/`restoreState` já usados em `MainActivity.kt:~213-218`).
  - Abre com o **mês mais recente com compras** selecionado (no design, o mês corrente); carrossel rola horizontalmente e o toque num card troca a lista abaixo.
  - Linha de tendência liga os totais dos meses visíveis; com 1 mês, só o ponto (sem polyline); altura normalizada por `max` dos totais (total 0 → base).
  - Cada linha mostra dia (`dd`), mês abreviado localizado, estabelecimento, subtítulo e valor formatado na moeda/locale do app (reutilizar o formatter já usado em `ShoppingItem.kt`/`Cart*`; localizar na implementação, **não** criar outro).
  - Estados: loading (skeleton/indicador), vazio ("Nenhuma compra finalizada ainda" + dica), erro com "Tentar de novo".
  - Toque em compra abre `CartFlow(shopping)`.
- **Performance:**
  - `LazyRow` (cards) e `LazyColumn` (compras) com `key` estável (`YearMonth`/`shopping.id`) e `contentType`.
  - Agrupamento fora da main (`flowOn(Dispatchers.Default)`); resultado em `StableList`/`ImmutableList` para evitar recomposição; `HistoryMonthTrend` calcula os pontos com `remember(months)` e desenha em `drawWithCache`/`Canvas` (sem alocar `Path` por frame).
  - **Sem I/O duplicado**: reutiliza o mesmo `observeAll()` já ativo em Listas (listener Firestore, cache offline); nenhuma query nova. Sem cache próprio em memória/DataStore — o Firestore offline persistence é a fonte.
  - `distinctUntilChanged` no flow de meses para não re-emitir quando só campos irrelevantes mudam.
- **Feature toggle:** nenhum. Mudança aditiva e sem risco de dados; rollback = não exibir a aba. *(Se o maintainer preferir rollout gradual:)*

  | Toggle | Default | Fonte | OFF | ON |
  |---|---|---|---|---|
  | `feat_history_tab` | true | remote-config | aba oculta | aba visível |

  Só criar se o maintainer pedir; sem ele não há novo remote-config.
- **Observabilidade:** `trackScreenView("history", "HistoryViewModel")` no `init` (mesmo padrão de `FinishPurchaseViewModel.kt:42-44`); eventos `history_viewed` (com `count` de compras), `history_month_selected`, `history_purchase_opened` (com `shopping_id`). Nenhum dado pessoal (estabelecimento/valor **não** vão nos params).
- **Acessibilidade/UI:** alvos ≥ 48dp; cada card de mês com `contentDescription` ("Novembro, R$ 486,30, selecionado"); `testTag`s: `history_screen`, `history_month_{yyyy-MM}`, `history_purchase_{id}`, `history_empty`, `history_error`, `history_retry`; cores/tipografia do `core:theme` (o design usa laranja `#C2410C`/`#FFEDD5` = tokens `primary`/`primaryContainer` do tema, **não** hex literal).
- **Compatibilidade:** minSdk 28 (`java.time` ok com desugaring já ativo — confirmar em `buildSrc`); docs antigos sem `finishedAt` desserializam com `null`; Wear OS não é afetado (campo opcional, `ShoppingModel` ignora chave desconhecida? — verificar `Json { ignoreUnknownKeys }` em `core:data` antes do merge).
- **Qualidade:** `lintDebug`, `detekt` (sem `@Suppress`), `testDebug`, `koverVerify koverVerifyLayout` ≥ 85%.

## 7. Plano de Testes

- **Unitários (JUnit + coroutines test, mocks do `ShoppingRepository`):**
  - `GroupHistoryByMonthUseCaseTest`: agrupa por mês; soma `price`; ordem crescente de meses e decrescente de compras; ignora `NEW`/`IN_PROGRESS`; `finishedAt` prevalece sobre `createdAt`; fallback para `createdAt`; `price = 0`; virada de ano (dez → jan); fuso (`ZoneId` fixo no teste, compra às 23:30 do último dia do mês); lista vazia.
  - `ObserveHistoryUseCaseTest`: repository emite lista vazia / com FINISH e sem FINISH / erro (flow lança → propaga para o ViewModel tratar).
  - `HistoryViewModelTest`: estado inicial `isLoading`; seleciona mês mais recente; `onMonthSelected` troca `selectedPurchases`; manter seleção quando o mesmo mês continua existindo após nova emissão; reset se o mês selecionado desapareceu; `onPurchaseClick` navega `CartFlow` e dispara evento; `onRetry` reassina; erro de rede → `error` e `isLoading=false`; `trackScreenView`/eventos chamados.
  - `ShoppingMapperTest`/`ShoppingModelTest`: round-trip com e sem `finishedAt`; `toMap()` inclui a chave.
  - `FinishPurchaseViewModelTest` (existe, alterado no working tree): `updateFields` recebe `finishedAt`.
  - `ShoppingReopenUseCaseTest`: envia `FieldOp.Delete` para `finishedAt`.
  - `ShoppingUpdateUseCaseTest`: `status=FINISH` (inclui "finished" em minúsculas) adiciona `finishedAt`; outros status não.
  - `ShoppingListViewModelTest`: `ShoppingAction.Update` com `FINISH` grava `finishedAt`.
  - `ArchitectureTest` (Konsist) deve continuar verde — sem novas exceções.
- **Instrumentados / UI (Compose, seletores só por `testTag`):** `HistoryContentTest` — estados loading/vazio/erro; toque em card de mês muda a lista; toque em compra dispara callback; um teste de `CestouBottomNavigation` com 3 itens e a seleção da aba Histórico (`core:ui` já tem `connectedDebugAndroidTest`).
- **Maestro (opcional, plano futuro `project_maestro_full_flow`):** fluxo "finalizar compra → aba Histórico mostra a compra", por `testTag`.
- **Comandos:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`, `./gradlew :core:ui:connectedDebugAndroidTest`.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Listas finalizadas **antes** desta versão não têm `finishedAt` e aparecem no mês de criação, que pode diferir do mês da compra | alta (só legado) | baixo (decisão do usuário: sem backfill) | Fallback `createdAt`; o erro some conforme novas compras são finalizadas; registrar no `CHANGELOG.md` |
| Esquecer um dos 3 caminhos de finalização (sheet, quick-finish, agente de IA) | média | médio (compra no mês errado) | Teste por caminho (§7); considerar centralizar num `FinishShoppingUseCase`/helper único (refactor opcional, fora do escopo) |
| `observeAll()` carrega todas as listas, inclusive abertas, só para filtrar `FINISH` | baixa | baixo | Base de usuários pequena; cache Firestore compartilhado com Listas. Se crescer, índice composto `users` + `status` + `finishedAt` (follow-up) |
| Cliente antigo (Wear/app antigo) regrava o doc inteiro (`ShoppingModel.toMap()`) sem `finishedAt` e apaga o campo | média | médio | Escritas parciais via `updateFields` já são a norma (`FinishPurchaseViewModel.kt:54-56`); `update()` com `toMap()` completo deve **preservar** o valor: o novo `toMap()` inclui `finishedAt` do modelo lido. Cobrir em teste |
| Linha/gráfico customizado em `Canvas` com semântica ruim (a11y) e custo de recomposição | média | baixo | `contentDescription` por card, `clearAndSetSemantics` no `Canvas`, `drawWithCache` |
| Conflito de merge com `sdd-028` (mesma branch `feat/notifications-bell-badge`, edita `ShoppingGraph.kt`, `AnalyticsEvents.kt`, strings) | alta | baixo | Implementar **depois** de `sdd-028` mergeado ou na mesma branch, em commits separados |
| `Json` do Wear/`core:data` rejeitar chave nova | baixa | alto | Verificar `ignoreUnknownKeys` antes do merge (§6 Compatibilidade) |

## 9. Perguntas em Aberto (bloqueantes)

Nenhuma pergunta bloqueante.

**Decidido pelo usuário (2026-11-02):**
- **Q1 — Data da compra:** criar `Shopping.finishedAt: Long?` (additive), gravado **automaticamente** (`System.currentTimeMillis()`) quando a lista passa a `FINISH`, valendo **apenas para finalizações futuras**. Sem backfill e sem campo de data na sheet "Finalizar compra" (`FinishPurchaseContent.kt` só tem valor e estabelecimento). Quem não tem `finishedAt` usa `createdAt` (`date = finishedAt ?: createdAt`).

*Não bloqueantes, assumidos (vetar na revisão):*
1. **Toque na compra** abre `CartFlow(shopping)` (carrinho da lista finalizada, já existente); F1 marca "Detalhe da compra (a definir)" — tela de detalhe própria seria plano separado.
2. Implementar dentro de `feature:shopping`, não em módulo novo.
3. Sem toggle remoto.
4. Wear OS fora do escopo.
5. Meses sem compras entre dois meses com compras **não** aparecem como card (só meses com ao menos uma compra).
6. Um campo de data editável na sheet pode vir depois gravando no mesmo `finishedAt`, sem retrabalho.

> `architecture/` não documenta histórico de compras (nenhuma regra a citar); lacuna registrada aqui, sem impacto na decisão.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existir — AD-009 para rules; área de histórico não documentada).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `additive` em "Impacto público" (`Shopping.finishedAt`, `History`, `BottomNavItem.History`).
- [x] Novo endpoint/remote-config: nenhum; toggle opcional definido e observabilidade planejada.
- [x] Novo cache: nenhum (Firestore offline persistence reaproveitada; justificado em §6).
- [x] Perguntas em aberto exauridas — Q1 decidida pelo usuário; demais itens são premissas não bloqueantes.
