# Task: Tela de Análise de gastos (gráfico de custo total + categorias que mais gastam)

- **Slug:** analise-gastos
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-02
- **Status:** ready
- **Versão-alvo:** 2.3.0 (minor — feature visível; mesmo release de `sdd-028`/`sdd-029`)
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive — nova rota `SpendingInsights : NavKey` em `core:navigation`. Nenhum modelo/contrato existente muda.

> **Dependência:** parte do código do `sdd-029` (aba Histórico, `Shopping.finishedAt`, `GroupHistoryByMonthUseCase`, `HistoryMonth`). Implementar **depois** do `sdd-029`.

## 1. Contexto e Motivação

- Pedido: "criar uma tela específica com gráfico de custo total, quais categorias são mais gastas e, caso queira detalhes, ele abre a lista atual".
- Interpretação (vetar se errada): é uma tela **à parte** do Histórico, aberta a partir dele; mostra (1) gráfico do custo total por mês, (2) ranking das categorias que mais pesaram no mês selecionado e (3) as compras daquele mês, e **tocar numa compra abre a lista dela** (`CartFlow`). Isso também fecha a Q2 do `sdd-029` (o toque do Histórico abre o `CartFlow`).
- O Histórico (`sdd-029`) já mostra total por mês e uma linha de tendência nos cards, mas **não** sabe nada de categorias nem tem gráfico próprio. O total do mês vem de `Shopping.price` (valor digitado em "Finalizar compra", `FinishPurchaseContent.kt:46,104`); a categoria só existe nos produtos (`Product.category`, `Product.kt:12`), na subcoleção `shopping/{id}/products`.
- **Não há artboard aprovado** no canvas Cestou para esta tela: o layout abaixo é proposta (wireframe §3) e deve ser validada visualmente antes do PR.
- Rastreabilidade: `Product.kt:7-22` (`price`, `quantity`, `isPurchased`, `category`, `total`), `ProductCategory.kt:21-49` (`core:ui`, enum com ícone/cor e `fromString`), `ProductReader.kt` / `ProductRepositoryImpl.kt:102-114` (`getAllProducts(shoppingId)`), `ShoppingGetDetailsUseCase.kt:37-51` (já soma `products.sumOf { it.total }` por lista), `ProductRepositoryImpl.kt:~87` (`checkIsLocked`: lista `FINISH` fica travada para escrita), `ShoppingGraph.kt:55-67`, `MobileRoutes.kt:15-30`.

## 2. Escopo

- **Inclui:**
  - Rota `SpendingInsights` e tela `SpendingInsightsScreen` (MVI), aberta por um botão/atalho "Ver análise" no topo da tela Histórico.
  - **Gráfico de custo total**: barras por mês (até os 6 meses mais recentes com compras), desenhadas em `Canvas`, mês selecionado destacado; toque na barra seleciona o mês.
  - **Ranking de categorias** do mês selecionado: barras horizontais ordenadas, com ícone/cor da `ProductCategory`, valor e % do total; top 5 + "Demais" agregado.
  - **Compras do mês** (mesmo item do Histórico); toque abre `CartFlow(shopping)`.
  - Estados loading / vazio / erro; strings pt-BR/en/es; `testTag`; Analytics.
- **Exclui:**
  - Alterar `Shopping`, `Product` ou Firestore (sem novos campos, sem denormalização — ver §8).
  - Comparativos (mês vs mês), metas, orçamento, exportação, filtro por pessoa/estabelecimento, intervalo livre de datas.
  - Paywall/PRO: a tela é gratuita (nenhuma decisão de billing foi pedida).
  - Wear OS.
  - Biblioteca de gráficos (sem nova dependência).

## 3. Impacto Arquitetural

- **Módulos/camadas:**
  - `:core:navigation` — `SpendingInsights` em `mobile/MobileRoutes.kt`.
  - `:feature:shopping` — `domain`, `presentation`, `navigation`, strings, `di`.
  - `:core:analytics` — constantes de eventos.
  - Nenhuma mudança em `:core:domain`, `:core:data`, `:feature:cart`, `:app`.
- **Decisão de módulo:** dentro de `feature:shopping` (mesmo raciocínio do `sdd-029`: consome `ShoppingRepository` e `ProductsUseCase`, ambos de `core:domain`; navega a `CartFlow` via `core:navigation`; sem dependência entre features). Respeita o `ArchitectureTest` (camadas `domain/data/presentation/navigation/di`).
- **Fronteira domain × UI (importante):** `ProductCategory` vive em `core:ui` e importa Compose; o `domain` **não pode** importá-lo (`ArchitectureTest` "domain does not depend on Android, Compose or Firebase"). Portanto:
  - o `domain` agrega por **chave de categoria crua** (`Product.category: String`, já normalizada com `trim().lowercase()`);
  - a `presentation` converte cada chave com `ProductCategory.fromString` e **funde** chaves que caem no mesmo enum (ex.: "Dairy" e "Laticínios" → `LATICINIOS`) somando valores, antes de ordenar e cortar o top 5.
- **Padrão:** Clean Architecture + MVI + Nav3.
- **Novos arquivos** (base `feature/shopping/src/main/java/br/com/brunocarvalhs/howmuch/feature/shopping/`):
  - `domain/model/SpendingModels.kt` (`MonthSpending`, `CategorySpending`)
  - `domain/usecase/ObserveMonthlySpendingUseCase.kt` (reaproveita `ObserveHistoryUseCase`/`GroupHistoryByMonthUseCase` do `sdd-029` → série de meses)
  - `domain/usecase/GetCategorySpendingUseCase.kt` (lê produtos das listas do mês, soma por categoria; com cache — §6)
  - `domain/usecase/CategorySpendingCache.kt` (cache em memória, `@Singleton`)
  - `presentation/state/SpendingInsightsUiState.kt`, `presentation/intent/SpendingInsightsIntent.kt`
  - `presentation/viewmodel/SpendingInsightsViewModel.kt`
  - `presentation/mapper/CategorySpendingUiMapper.kt` (chave crua → `ProductCategory`, fusão, top 5 + "Demais")
  - `presentation/screen/SpendingInsightsScreen.kt` (+ `SpendingInsightsContent` sem ViewModel)
  - `presentation/components/insights/MonthlySpendingChart.kt`, `CategoryRankingRow.kt`, `SpendingInsightsEmptyState.kt`
  - Reuso de `HistoryPurchaseRow` (`sdd-029`).
  - Testes espelhando cada item (§7).
- **Arquivos alterados:** `MobileRoutes.kt`, `ShoppingGraph.kt`, `HistoryScreen.kt`/`HistoryIntent.kt`/`HistoryViewModel.kt` (atalho "Ver análise", do `sdd-029`), `AnalyticsEvents.kt`, `strings.xml` x3 de `feature:shopping`.

```
 HistoryScreen ──[Ver análise]──► composable<SpendingInsights>
                                        │ hiltViewModel
                                        ▼
                               SpendingInsightsViewModel ◄── intent: selectMonth / openPurchase / retry
                                  │            │
        ObserveMonthlySpending ◄──┘            └──► GetCategorySpendingUseCase(month)
        (HistoryMonth[] → série)                      │  para cada lista FINISH do mês:
                                                      │  ProductsUseCase(shoppingId).first()  ← cache por (id, finishedAt)
                                                      ▼
                                           Map<chaveCrua, Double>  ──► UiMapper ──► ProductCategory (fusão + top 5)

 toque numa compra ──► navigator.navigate(CartFlow(shopping))
```

Wireframe (proposta, sem artboard aprovado):

```
 ← Análise de gastos
 ┌──────────────────────────────────┐
 │ Custo total por mês              │
 │   ▂    ▅    ▇(sel)               │   barras Canvas, rótulo do mês embaixo,
 │  SET  OUT  NOV     R$ 486,30     │   valor do mês selecionado acima
 ├──────────────────────────────────┤
 │ Onde mais gastou em novembro     │
 │ 🥩 Carnes      ████████  R$ 142 29%│
 │ 🍞 Mercearia   █████     R$  96 20%│
 │ …  Demais      ██        R$  41  8%│
 ├──────────────────────────────────┤
 │ Compras de novembro              │
 │ [12 nov] Atacadão   R$ 186,40  > │  → abre a lista
 └──────────────────────────────────┘
```

- **Hilt:** use cases/ViewModel com `@Inject constructor`; `CategorySpendingCache` `@Singleton` com `@Inject constructor()`. `Clock` reaproveitado do `sdd-029`.
- **Threading:** leituras de produtos em paralelo limitado (4 coroutines, `Semaphore`) em `Dispatchers.IO`; agregação em `Dispatchers.Default`; coleta na UI com `collectAsStateWithLifecycle`.

## 4. Contratos e Interfaces

(Assinaturas apenas; tudo `internal` — nada sai do módulo, justificável porque só `shoppingGraph` e a própria feature usam estes tipos.)

- **`SpendingInsights : NavKey, RouteProtocol` (`core:navigation`, `public`)** — `data object SpendingInsights`, `routeType = PROTECTED`. `public` porque `feature:shopping` registra a rota e `core:navigation` é o único lugar de rotas compartilhadas (mesmo critério de `Notifications`/`Profile`, `MobileRoutes.kt:15-30`). Consumidor: `ShoppingGraph` e `HistoryViewModel` (via `Navigator`).
- **Domain:**
  - `data class MonthSpending(val key: YearMonth, val total: Double, val purchaseCount: Int)`
  - `data class CategorySpending(val rawKey: String, val total: Double)`
  - `class GetCategorySpendingUseCase @Inject constructor(productsUseCase: ProductsUseCase, cache: CategorySpendingCache) { suspend operator fun invoke(purchases: List<Shopping>): Result<List<CategorySpending>> }`
  - `class ObserveMonthlySpendingUseCase @Inject constructor(observeHistory: ObserveHistoryUseCase) { operator fun invoke(limit: Int = 6): Flow<List<HistoryMonth>> }`
- **Presentation:**
  - `data class SpendingInsightsUiState(val isLoading: Boolean = true, val months: StableList<MonthSpending>, val selectedMonth: YearMonth?, val categories: StableList<CategoryShare>, val categoriesLoading: Boolean, val purchases: StableList<HistoryPurchase>, val error: UiText?)`
  - `data class CategoryShare(val category: ProductCategory?, val total: Double, val percent: Float)` — `category == null` representa "Demais".
  - `interface SpendingInsightsIntent { fun onMonthSelected(key: YearMonth); fun onPurchaseClick(purchase: HistoryPurchase); fun onBack(); fun onRetry() }`
- **Analytics (`AnalyticsEvents.kt`):** `SPENDING_INSIGHTS_VIEWED = "spending_insights_viewed"`, `SPENDING_INSIGHTS_MONTH_SELECTED = "spending_insights_month_selected"`, `SPENDING_INSIGHTS_LIST_OPENED = "spending_insights_list_opened"`; params reaproveitam `SHOPPING_ID`/`MONTH` (`sdd-029`). Sem valores nem nomes de categoria/estabelecimento nos params.
- **Firestore/remote-config:** nenhum campo, coleção ou índice novo. Lê `shopping/{id}/products` já existente (`firestore.rules:~201`, acesso por membership do pai).

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | O total de uma compra é o `price` digitado ao finalizar (default = estimativa) | **Gráfico de custo total** soma `Shopping.price` das listas `FINISH` do mês — o mesmo número do Histórico | `FinishPurchaseContent.kt:46,104`; `FinishPurchaseViewModel.kt:57-66` |
| 2 | O valor por item é `price * quantity`; `price` pode ser `null` (vale 0) | **Ranking de categorias** soma `Product.total` por categoria; produtos sem preço contam 0 | `Product.kt:15,17`; `extensions/orEmpty` |
| 3 | `Product.isPurchased` marca o que foi pego no mercado; ao finalizar, itens não marcados podem existir | Premissa: só entram no ranking produtos com `isPurchased == true`. (Vetar: alternativa é contar todos) | `Product.kt:11`; `ShoppingGetDetailsUseCase.kt:44` (hoje soma todos) |
| 4 | `Shopping.price` é editável e pode diferir da soma dos produtos | Ranking mostra **% sobre a soma dos produtos** (não sobre `Shopping.price`) e, se a soma divergir, exibe o rodapé "baseado nos itens com preço"; o gráfico mensal segue usando `Shopping.price` | `FinishPurchaseContent.kt:104` |
| 5 | `Product.category` é `String` livre, com default `"Outros"` e valores vindos de IA/idiomas diferentes | Domain agrega por chave crua normalizada; UI funde via `ProductCategory.fromString` (fallback `OUTROS`) | `Product.kt:12`; `ProductCategory.kt:~46-57` |
| 6 | Lista `FINISH` fica travada para escrita de produtos | Produtos de listas finalizadas são estáveis ⇒ podem ser cacheados por `(shoppingId, finishedAt)`; reabrir/refinalizar muda a chave e invalida | `ProductRepositoryImpl.kt:~87` (`checkIsLocked`) |
| 7 | Mês de uma compra = `finishedAt ?: createdAt` (`sdd-029`) | Mesmo critério de data nesta tela (reuso de `GroupHistoryByMonthUseCase`) | `.specs/sdd-029-tela-historico/tasks.md` §5 regra 2 |
| 8 | Toque numa lista de compras abre o carrinho | Toque na compra abre `CartFlow(shopping)` | `ShoppingGraph.kt` / `ShoppingListViewModel.kt:~263` |

## 6. Critérios de Aceitação

- **Funcional:**
  - Do Histórico, "Ver análise" abre a tela; voltar retorna ao Histórico mantendo mês selecionado e scroll.
  - Barra do mês selecionado destacada; trocar o mês atualiza ranking e lista de compras.
  - Ranking ordenado por valor desc., top 5 + "Demais"; % somam 100% (±arredondamento); categoria sem valor não aparece.
  - Meses sem compras não geram barra; com 1 mês, 1 barra.
  - Valores na moeda/locale do app (formatter já existente, o mesmo do `sdd-029`).
  - Estados: loading do gráfico e loading independente do ranking (`categoriesLoading`), vazio ("Finalize uma compra para ver sua análise"), erro com "Tentar de novo" (erro no ranking não derruba o gráfico).
- **Performance / custo de leitura (ponto crítico):**
  - O ranking exige ler os produtos de **cada lista do mês**: 1 leitura por documento de produto. Mitigações obrigatórias: (a) carrega **só o mês selecionado**, nunca os 6; (b) `ProductsUseCase(id).first()` — snapshot único, sem manter listener aberto; (c) paralelismo máx. 4; (d) **cache em memória** `CategorySpendingCache` por `(shoppingId, finishedAt)` — memória, não DataStore/Room, porque os dados são derivados, pequenos e baratos de recomputar, e o cache do Firestore já cobre o offline; (e) trocar de mês e voltar não relê.
  - Cancelamento: ao trocar de mês, o `Job` anterior é cancelado (`collectLatest`/`flatMapLatest`).
  - UI: `Canvas` com `drawWithCache` e pontos em `remember(months)`; `LazyColumn` com `key`; estado em `StableList`.
- **Feature toggle:** nenhum (aditivo, sem escrita). Rollback = ocultar o botão "Ver análise".
- **Observabilidade:** `trackScreenView("spending_insights", "SpendingInsightsViewModel")` no `init`; eventos de §4; falha de leitura de produtos registrada com `Timber.e` (sem dados pessoais) e refletida em `error`.
- **Acessibilidade/UI:** alvos ≥ 48dp; cada barra com `contentDescription` ("Novembro, R$ 486,30, selecionado"); ranking com semântica de lista; **cor nunca é a única pista** (valor e % em texto); cores de categoria vêm de `ProductCategory.color`, demais do tema (`primary`/`primaryContainer`, sem hex literal); contraste mínimo 4.5:1 do texto sobre as barras. `testTag`s: `insights_screen`, `insights_month_{yyyy-MM}`, `insights_category_{n}`, `insights_purchase_{id}`, `insights_empty`, `insights_error`, `insights_retry`.
- **Compatibilidade:** minSdk 28; sem migração de dados; Wear OS inalterado.
- **Qualidade:** `lintDebug`, `detekt` (sem `@Suppress`), `testDebug`, `koverVerify koverVerifyLayout` ≥ 85%.

## 7. Plano de Testes

- **Unitários (JUnit + coroutines test; mocks de `ProductsUseCase`/`ShoppingRepository`):**
  - `GetCategorySpendingUseCaseTest`: soma por categoria; ignora `isPurchased=false`; `price=null` ⇒ 0; várias listas no mesmo mês; categoria com maiúsculas/espaços unificada; lista sem produtos; **falha de uma lista** (as outras ainda somam? definir: falha total ⇒ `Result.failure`, coberto); cache hit não chama `ProductsUseCase` de novo; chave muda com `finishedAt` novo ⇒ relê; paralelismo ≤ 4 (contagem de concorrência).
  - `CategorySpendingCacheTest`: put/get, invalidação por chave diferente.
  - `CategorySpendingUiMapperTest`: fusão de chaves no mesmo enum ("Dairy"+"Laticínios"); `fromString` desconhecido ⇒ `OUTROS`; top 5 + "Demais"; percentuais; total 0 ⇒ lista vazia sem divisão por zero; empate de valores com ordem estável.
  - `ObserveMonthlySpendingUseCaseTest`: limita a 6 meses mais recentes; ordem crescente.
  - `SpendingInsightsViewModelTest`: estado inicial; seleciona o mês mais recente; `onMonthSelected` cancela a carga anterior e carrega a nova; erro só no ranking mantém o gráfico; `onRetry`; `onPurchaseClick` navega `CartFlow` e emite evento; `trackScreenView`.
  - `ArchitectureTest` (Konsist) continua verde: o `domain` novo não importa `core.ui`.
- **Instrumentados / UI (Compose, só `testTag`):** `SpendingInsightsContentTest` — loading/vazio/erro; toque na barra seleciona o mês; toque na compra dispara callback; ranking exibe "Demais".
- **Comandos:** `./gradlew lintDebug`, `./gradlew detekt`, `./gradlew testDebug`, `./gradlew koverVerify koverVerifyLayout`, `./gradlew :feature:shopping:connectedDebugAndroidTest`.

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Custo de leitura Firestore: 1 leitura por produto de cada lista do mês | média | médio (custo e latência) | Só mês selecionado, snapshot único, cache por `(id, finishedAt)`, paralelismo 4, loading independente. Alternativa futura: denormalizar `categoryTotals` no doc da lista ao finalizar (zero leituras, mas muda contrato e não cobre histórico — **rejeitada agora**, fica como follow-up se o custo aparecer) |
| Soma dos produtos diverge de `Shopping.price` (usuário edita o total ao finalizar) | alta | médio (números "não batem") | Regra 4: % sobre a soma dos produtos + rodapé explicativo; gráfico mensal usa `Shopping.price` |
| `Product.category` livre (IA, idiomas) fragmenta o ranking | média | médio | Normalização no domain + fusão por `ProductCategory.fromString` na UI; testes de fusão |
| Listas sem preços nos produtos geram ranking vazio/zero | média | baixo | Estado "sem preços" no ranking (mensagem), gráfico mensal continua |
| `domain` tentando usar `ProductCategory` (core:ui) quebra o Konsist | baixa | médio | Fronteira definida em §3; teste de arquitetura |
| Conflito de merge com `sdd-028`/`sdd-029`/`sdd-030` (`ShoppingGraph.kt`, `AnalyticsEvents.kt`, strings) | alta | baixo | Implementar após `sdd-029` mergeado; commits separados |
| Sem artboard aprovado ⇒ retrabalho visual | média | baixo | Wireframe §3 + validação visual antes do PR; ajustes são só de `presentation` |

## 9. Perguntas em Aberto (bloqueantes)

Nenhuma pergunta bloqueante.

*Premissas não bloqueantes (vetar na revisão):*
1. "Abre a lista atual" = tocar numa **compra** abre o `CartFlow` da lista dela (não "a lista em andamento do usuário").
2. Tela separada, acessada pelo Histórico (não nova aba na barra inferior).
3. Ranking conta só produtos `isPurchased == true` (regra 3).
4. Gráfico em barras por mês (até 6 meses); seleção por mês, sem intervalo livre.
5. Sem paywall/PRO.
6. Layout do §3 é proposta; sem artboard aprovado.

> `architecture/` não documenta agregação de gastos nem categorias de produto; lacuna registrada aqui, sem impacto na decisão.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (e `architecture/` quando existir — área não documentada, registrada).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas/wireframe).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `additive` em "Impacto público" (`SpendingInsights`).
- [x] Novo endpoint/remote-config: nenhum; toggle dispensado e justificado; observabilidade planejada.
- [x] Novo cache: memória, justificado (dados derivados, baratos, Firestore offline já cobre persistência).
- [x] Perguntas em aberto exauridas — sem bloqueantes; premissas listadas.
