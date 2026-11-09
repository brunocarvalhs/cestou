# Task: Expansão de AppFunctions - Tier 1 e Tier 2 (Busca, Automação, Analytics)

- **Slug:** appfunctions-tier1-tier2-comprehensive
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-08
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação

Sdd-046 + sdd-047 expuseram 10 funções básicas (CRUD). Para potencializar a IA, faltam:
- **Tier 1:** Busca, adição em lote, listagem, detalhes, limpeza (casos comuns)
- **Tier 2:** Duplicação, receitas, analytics, histórico, renomeação (automação avançada)

Demanda: "Foca no tier 1 e 2" — implementar 10 novos AppFunctions (2026-11-08).

Rastreabilidade:
- Use cases: `feature/products/domain/usecase/ProductSearchUseCase.kt`, `ProductListSaveUseCase.kt`, `CommonProductAddAllToShoppingUseCase.kt`, `ShoppingGetAllUseCase.kt`, `ShoppingGetDetailsUseCase.kt`, `ShoppingClearPurchasedUseCaseImpl.kt`, `ShoppingDuplicateUseCase.kt`, `RecipeAddToListUseCase.kt`, `ObserveMonthlySpendingUseCase.kt`, `ObserveHistoryUseCase.kt`, `ShoppingUpdateUseCase.kt`
- AppFunctions base: `core/appfunctions/functions/` (sdd-046 + sdd-047)

## 2. Escopo

- **Inclui:**
  - **Tier 1 (5):** SearchProductFunction, AddProductsMultipleFunction, ListAllShoppingListsFunction, GetShoppingDetailsFunction, ClearPurchasedItemsFunction
  - **Tier 2 (5):** DuplicateShoppingListFunction, RecipeToShoppingListFunction, GetMonthlySpendingFunction, GetSpendingHistoryFunction, RenameShoppingListFunction
  - Feature flags: `feat_app_functions_tier1`, `feat_app_functions_tier2` (both default: on)
  - Integração Analytics + Crashlytics (padrão sdd-046/047)
  - 30 testes unitários (3 cenários × 10 funções)

- **Exclui:**
  - Alterações em UI/Navigation
  - Suporte Wear OS específico
  - Sincronização offline
  - Integração com receitas em profundidade (apenas `RecipeAddToListUseCase`)

## 3. Impacto Arquitetural

**Módulos afetados:**
- `core:appfunctions` — 10 novas funções (functions/)
- `core:remote-config` — 2 novas flags (RemoteConfigKeys.kt)
- `core:analytics` — uso de evento existente (`APP_FUNCTION_INVOKED`)

**Padrão aplicado:** Clean Architecture + AppFunctions (igual sdd-046/047)

**Novos arquivos:**

```
core/appfunctions/src/main/java/br/com/brunocarvalhs/howmuch/core/appfunctions/
├── functions/
│   ├── SearchProductFunction.kt
│   ├── AddProductsMultipleFunction.kt
│   ├── ListAllShoppingListsFunction.kt
│   ├── GetShoppingDetailsFunction.kt
│   ├── ClearPurchasedItemsFunction.kt
│   ├── DuplicateShoppingListFunction.kt
│   ├── RecipeToShoppingListFunction.kt
│   ├── GetMonthlySpendingFunction.kt
│   ├── GetSpendingHistoryFunction.kt
│   └── RenameShoppingListFunction.kt
core/appfunctions/src/test/java/.../functions/
├── SearchProductFunctionTest.kt
├── AddProductsMultipleFunctionTest.kt
├── ListAllShoppingListsFunctionTest.kt
├── GetShoppingDetailsFunctionTest.kt
├── ClearPurchasedItemsFunctionTest.kt
├── DuplicateShoppingListFunctionTest.kt
├── RecipeToShoppingListFunctionTest.kt
├── GetMonthlySpendingFunctionTest.kt
├── GetSpendingHistoryFunctionTest.kt
└── RenameShoppingListFunctionTest.kt
```

**Diagrama de fluxo:**

```
┌────────────────────────────────────────────────────────────┐
│ Gemini / System Assistant (Android 16+)                    │
└────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────────┐
│ 20 AppFunctions Totais (sdd-046 + sdd-047 + sdd-048)                │
├─────────────────────────────────────────────────────────────────────┤
│ TIER 1 (5):  Search, AddMultiple, ListAll, GetDetails, ClearPurch  │
│ TIER 2 (5):  Duplicate, Recipe, MonthlySpend, SpendHistory, Rename │
│ TOTAL (10): ↑ NEW (sdd-048)                                         │
├─────────────────────────────────────────────────────────────────────┤
│ +10 anteriores (sdd-046/047): Create, Add, Summary, Mark, Remove, etc
└─────────────────────────────────────────────────────────────────────┘
                           ↓
┌────────────────────────────────────────────────────────────────────┐
│ CestouAppFunctionsService                                          │
├────────────────────────────────────────────────────────────────────┤
│ + isEnabled(tier1/tier2) → FeatureFlagService                     │
│ + trackFunctionInvoked() → AnalyticsTracker                       │
│ + recordError() → FirebaseCrashlytics                             │
└────────────────────────────────────────────────────────────────────┘
                           ↓
┌────────────────────────────────────────────────────────────────────┐
│ Use Cases (Domain Layer)                                           │
├────────────────────────────────────────────────────────────────────┤
│ ProductSearchUseCase, CommonProductAddAllToShoppingUseCase,        │
│ ShoppingGetAllUseCase, ShoppingGetDetailsUseCase,                  │
│ ShoppingClearPurchasedUseCaseImpl, ShoppingDuplicateUseCase,        │
│ RecipeAddToListUseCase, ObserveMonthlySpendingUseCase,             │
│ ObserveHistoryUseCase, ShoppingUpdateUseCase                       │
└────────────────────────────────────────────────────────────────────┘
```

**Injeção via Hilt:**
- Cada função Tier 1/2 recebe: `ShoppingRepository`, `ProductsUseCase`, `AnalyticsTracker`, `FirebaseCrashlytics`, + use cases específicos
- `CestouAppFunctionsService` expõe `isEnabled(tier)` para feature flag check

## 4. Contratos e Interfaces

### TIER 1: SearchProductFunction

```
suspend fun searchProduct(
    shoppingId: String,
    query: String              // query pode ser nome ou categoria
): String  // "Found 3 products: Milk (qty 1), Butter (qty 2), ..."
```

**Origem:** `ProductSearchUseCase.kt` — invoke(shoppingId, query) retorna Flow<List<Product>>

### TIER 1: AddProductsMultipleFunction

```
suspend fun addProductsMultiple(
    shoppingId: String,
    products: String           // JSON array ou CSV: "arroz,1,2.50;feijão,1,3.00"
): String  // "Added 2 products successfully"
```

**Origem:** `ProductListSaveUseCase.kt` + `CommonProductAddAllToShoppingUseCase.kt`

### TIER 1: ListAllShoppingListsFunction

```
suspend fun listAllShoppingLists(): String
// "2 lists: [Weekly (5 items, $42.50)] [Monthly (12 items, $150.00)]"
```

**Origem:** `ShoppingGetAllUseCase.kt` → getAll() retorna List<Shopping>

### TIER 1: GetShoppingDetailsFunction

```
suspend fun getShoppingDetails(shoppingId: String): String
// "Weekly | Budget: $100 | Spent: $42.50 | Members: 2 | Items: 5"
```

**Origem:** `ShoppingGetDetailsUseCase.kt` → getDetails(shoppingId) retorna Shopping

### TIER 1: ClearPurchasedItemsFunction

```
suspend fun clearPurchasedItems(shoppingId: String): String
// "Removed 3 purchased items"
```

**Origem:** `ShoppingClearPurchasedUseCaseImpl.kt` — clearPurchased(shoppingId)

---

### TIER 2: DuplicateShoppingListFunction

```
suspend fun duplicateShoppingList(
    shoppingId: String,
    newTitle: String? = null   // null = original + " (Copy)"
): String  // "List duplicated to 'Weekly (Copy)' with 5 items"
```

**Origem:** `ShoppingDuplicateUseCase.kt` — duplicate(shopping, newTitle?)

### TIER 2: RecipeToShoppingListFunction

```
suspend fun recipeToShoppingList(
    shoppingId: String,
    recipeQuery: String        // ex: "bolo de chocolate"
): String  // "Added 6 ingredients for 'Chocolate Cake'"
```

**Origem:** `RecipeAddToListUseCase.kt` + `RecipeSearchUseCase.kt` — search(query) + addAll(shoppingId, ingredients)

### TIER 2: GetMonthlySpendingFunction

```
suspend fun getMonthlySpending(
    month: String? = null      // "2026-11" ou null (atual)
): String  // "November 2026: $523.50 in 12 lists"
```

**Origem:** `ObserveMonthlySpendingUseCase.kt` — observe(month) retorna Flow<Double>

### TIER 2: GetSpendingHistoryFunction

```
suspend fun getSpendingHistory(
    months: Int = 3            // últimos N meses
): String  // "Sept: $400 | Oct: $450 | Nov: $523"
```

**Origem:** `ObserveHistoryUseCase.kt` + `GroupHistoryByMonthUseCase.kt` — observe() + groupByMonth()

### TIER 2: RenameShoppingListFunction

```
suspend fun renameShoppingList(
    shoppingId: String,
    newTitle: String
): String  // "List renamed to 'Natal 2026'"
```

**Origem:** `ShoppingUpdateUseCase.kt` — update(shopping.copy(title = newTitle))

---

### Alteração em RemoteConfigKeys.kt

```kotlin
const val APP_FUNCTIONS_TIER1_ENABLED = "feat_app_functions_tier1"
const val APP_FUNCTIONS_TIER2_ENABLED = "feat_app_functions_tier2"
```

Todas funções marcadas como `public` — justificativa: exposição necessária para descoberta por agentes de IA.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | Apenas proprietário pode alterar lista | AppFunctions respeitam roles/permissões do usuário autenticado | `ShoppingRepository.kt:1-50` |
| 2 | Busca é por nome ou categoria | SearchProductFunction retorna combinação de ambos com highlights | `ProductSearchUseCase.kt:1-40` |
| 3 | Duplicação cria cópia com itens | DuplicateShoppingListFunction usa ShoppingDuplicateUseCase | `ShoppingDuplicateUseCase.kt:1-35` |
| 4 | Histórico agrupa por mês | GetSpendingHistoryFunction retorna breakdown mensal | `GroupHistoryByMonthUseCase.kt:1-30` |
| 5 | Receitas são templates opcionais | RecipeToShoppingListFunction falha gracefully se receita não existe | `RecipeAddToListUseCase.kt:1-40` |

## 6. Critérios de Aceitação

**Performance:**
- Cada AppFunction executa em <200ms (SearchProduct, GetMonthlySpending podem ser mais lentos se houver muitos dados, máximo 500ms)
- Sem alocações desnecessárias; reutiliza Flows/streams existentes
- Sem I/O duplicado (cache de resultado imediato em memory se invocado em sequência)
- Sem recomposições (AppFunctions não são Composables)

**Feature Toggles:**

| Toggle | Default | Fonte | OFF | ON |
|---|---|---|---|---|
| `feat_app_functions_tier1` | true | RemoteConfig | Tier 1 funcs (5) desabilitadas | ✓ |
| `feat_app_functions_tier2` | true | RemoteConfig | Tier 2 funcs (5) desabilitadas | ✓ |

**Observabilidade:**
- Evento: `app_function_invoked` (reutilizado de sdd-046/047)
- Parâmetros: `action_id` (nome da função), `success`, `reason` (se erro), `tier` (1 ou 2)
- Logs estruturados: `crashlytics.log()` com contexto
- Exceções brutas: `crashlytics.recordException()`

**Compatibilidade:**
- Android 16+ (AppFunctions API)
- Zero breaking changes em `core:domain` ou features existentes

## 7. Plano de Testes

**Unitários (30 testes = 3 cenários × 10 funções):**

Cada função testa:
1. **Happy path:** sucesso com dados válidos
2. **Validação:** lista não existe, query vazia, receita não encontrada, etc.
3. **Erro/Permissão:** quando aplicável (ex.: user sem acesso à lista)

Exemplo: `SearchProductFunctionTest`
- ✓ Busca encontra produtos
- ✗ Query vazia → erro
- ✗ Lista não existe → erro

Exemplo: `GetMonthlySpendingFunctionTest`
- ✓ Retorna total do mês
- ✗ Mês inválido → erro
- ✗ Nenhum dado para o mês → 0

**Lint/Detekt/Cobertura:**
- `./gradlew lintDebug` — sem findings
- `./gradlew detekt` — sem findings
- `./gradlew testDebug` — 30 testes ✓
- `./gradlew koverVerify` — cobertura >85% nos novos arquivos

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| ProductSearchUseCase retorna dados lentos | Baixa | Média | Timeout <200ms em busca, fallback para lista vazia |
| Receita não encontrada em RecipeToShoppingListFunction | Média | Baixa | Tratamento graceful, mensagem clara ao usuário |
| Feature flag toggle OFF quebra Gemini mid-flow | Baixa | Alta | Toggle é kill-switch remoto, testes com toggle OFF |
| Histórico muito grande em GetSpendingHistoryFunction | Baixa | Média | Paginação implícita (últimos N meses, padrão 3) |

## 9. Perguntas em Aberto (bloqueantes)

- [ ] Nenhuma.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (`ProductSearchUseCase.kt`, `ShoppingDuplicateUseCase.kt`, etc.).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [x] Seção 6 preenchida de forma substantiva (performance, toggles, observabilidade, compatibilidade).
- [x] Mudança em símbolo público reflete `additive` em "Impacto público" (10 novas funções).
- [x] Novos toggles definidos: `feat_app_functions_tier1`, `feat_app_functions_tier2`, observabilidade planejada.
- [x] Sem novo cache — reutiliza use cases/repositórios existentes.
- [x] Perguntas em aberto exauridas.
