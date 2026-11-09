# Task: Expansão de AppFunctions - Tier 3 (Casos Avançados & Utilitários)

- **Slug:** appfunctions-tier3-advanced
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-08
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação

Sdd-046 + sdd-047 + sdd-048 expuseram 15 funções. O Tier 3 fecha a superfície de API com **5 casos avançados e utilitários** (atualização de preço, duplicação de produto, reabertura de lista, listar membros, gerar código de compartilhamento).

Demanda: "pode fazer o tier 3" — implementar as 5 últimas AppFunctions (2026-11-08).

Rastreabilidade:
- Use cases: `feature/products/domain/usecase/ProductsUseCase.kt` (update), `ShoppingDuplicateUseCase.kt`, `ShoppingReopenUseCase.kt`, `ShoppingGetDetailsUseCase.kt` (para membros)
- Repositórios: `core/domain/repository/ShoppingRepository.kt:1-50`, `ProductsUseCase.kt:1-40`
- AppFunctions base: `core/appfunctions/functions/` (sdd-046 + sdd-047 + sdd-048)

## 2. Escopo

- **Inclui:**
  - **Tier 3 (5):** UpdateProductPriceFunction, DuplicateProductFunction, ReopenShoppingListFunction, ListListMembersFunction, GenerateShareCodeFunction
  - Feature flag: `feat_app_functions_tier3` (default: on)
  - Integração Analytics + Crashlytics (padrão sdd-046/047/048)
  - 15 testes unitários (3 cenários × 5 funções)

- **Exclui:**
  - Alterações em UI/Navigation
  - Suporte Wear OS específico
  - Sincronização offline

## 3. Impacto Arquitetural

**Módulos afetados:**
- `core:appfunctions` — 5 novas funções (functions/)
- `core:remote-config` — 1 nova flag (RemoteConfigKeys.kt)
- `core:analytics` — uso de evento existente (`APP_FUNCTION_INVOKED`)

**Padrão aplicado:** Clean Architecture + AppFunctions (igual sdd-046/047/048)

**Novos arquivos:**

```
core/appfunctions/src/main/java/br/com/brunocarvalhs/howmuch/core/appfunctions/
├── functions/
│   ├── UpdateProductPriceFunction.kt
│   ├── DuplicateProductFunction.kt
│   ├── ReopenShoppingListFunction.kt
│   ├── ListListMembersFunction.kt
│   └── GenerateShareCodeFunction.kt
core/appfunctions/src/test/java/.../functions/
├── UpdateProductPriceFunctionTest.kt
├── DuplicateProductFunctionTest.kt
├── ReopenShoppingListFunctionTest.kt
├── ListListMembersFunctionTest.kt
└── GenerateShareCodeFunctionTest.kt
```

**Diagrama de fluxo:**

```
┌────────────────────────────────────────────────────────┐
│ 25 AppFunctions Totais (sdd-046 + sdd-047 + sdd-048 + sdd-049)
├────────────────────────────────────────────────────────┤
│ TIER 1 (5):   Search, AddMultiple, ListAll, GetDetails, ClearPurch
│ TIER 2 (5):   Duplicate, Recipe, MonthlySpend, History, Rename
│ TIER 3 (5):   UpdatePrice, DuplicateProd, Reopen, ListMembers, ShareCode ← NEW
│ +10 (sdd-046/047): Create, Add, Summary, Mark, Remove, etc
└────────────────────────────────────────────────────────┘
                           ↓
┌────────────────────────────────────────────────────────┐
│ CestouAppFunctionsService                              │
├────────────────────────────────────────────────────────┤
│ + isEnabled(tier1/tier2/tier3) → FeatureFlagService   │
│ + trackFunctionInvoked() → AnalyticsTracker            │
│ + recordError() → FirebaseCrashlytics                  │
└────────────────────────────────────────────────────────┘
```

**Injeção via Hilt:**
- Cada função Tier 3 recebe: `ShoppingRepository`, `ProductsUseCase`, `AnalyticsTracker`, `FirebaseCrashlytics`
- Use cases específicos para cada função quando necessário

## 4. Contratos e Interfaces

### UpdateProductPriceFunction

```
suspend fun updateProductPrice(
    shoppingId: String,
    productId: String,
    newPrice: Float
): String  // "Product price updated to $X.XX"
```

**Origem:** `ProductsUseCase.update()` → `productsUseCase.update(product.copy(price = newPrice), shoppingId)`

### DuplicateProductFunction

```
suspend fun duplicateProduct(
    shoppingId: String,
    productId: String,
    quantity: Double = 1.0
): String  // "Product duplicated with quantity X"
```

**Origem:** `ProductsUseCase.save()` → cria nova instância com novo ID

### ReopenShoppingListFunction

```
suspend fun reopenShoppingList(
    shoppingId: String
): String  // "Shopping list reopened"
```

**Origem:** `ShoppingReopenUseCase` → reopen(shoppingId)

### ListListMembersFunction

```
suspend fun listListMembers(
    shoppingId: String
): String  // "3 members: user1 (owner), user2 (editor), user3 (viewer)"
```

**Origem:** `ShoppingGetDetailsUseCase.getDetails()` → parse users + roles

### GenerateShareCodeFunction

```
suspend fun generateShareCode(
    shoppingId: String
): String  // "Share code: ABC123 (expires in 30 days)"
```

**Origem:** `ShoppingRepository.ensureShortCodeIndex()` → gera ou retorna código existente

---

### Alteração em RemoteConfigKeys.kt

```kotlin
const val APP_FUNCTIONS_TIER3_ENABLED = "feat_app_functions_tier3"
```

Todas funções marcadas como `public` — justificativa: exposição necessária para descoberta por agentes de IA.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | Apenas proprietário pode alterar produto | AppFunctions respeitam roles/permissões | `ProductsUseCase.kt:1-40` |
| 2 | Produto tem preço único | UpdateProductPriceFunction permite ajuste dinâmico | `Product.kt:7-27` |
| 3 | Produto tem ID imutável | DuplicateProductFunction gera novo ID | `Product.kt:1-10` |
| 4 | Lista finalizada fica imutável | ReopenShoppingListFunction permite reabertura | `ShoppingReopenUseCase.kt:1-30` |
| 5 | Membros e papéis gerenciados | ListListMembersFunction expõe estrutura | `Shopping.kt:15-25` |
| 6 | Código de compartilhamento é persistente | GenerateShareCodeFunction cria ou retorna existente | `ShoppingRepository.kt:40-50` |

## 6. Critérios de Aceitação

**Performance:**
- Cada AppFunction executa em <200ms (atualização de preço pode ser mais rápida ~50ms)
- Sem alocações desnecessárias; reutiliza estruturas existentes
- Sem I/O duplicado

**Feature Toggle:**

| Toggle | Default | Fonte | OFF | ON |
|---|---|---|---|---|
| `feat_app_functions_tier3` | true | RemoteConfig | Tier 3 funcs (5) desabilitadas | ✓ |

**Observabilidade:**
- Evento: `app_function_invoked` (reutilizado)
- Parâmetros: `action_id`, `success`, `reason`, `tier`
- Logs estruturados: `crashlytics.log()` com contexto
- Exceções: `crashlytics.recordException()`

**Compatibilidade:**
- Android 16+ (AppFunctions API)
- Zero breaking changes

## 7. Plano de Testes

**Unitários (15 testes = 3 cenários × 5 funções):**

Cada função testa:
1. **Happy path:** sucesso com dados válidos
2. **Validação:** entrada inválida, recurso não encontrado
3. **Erro/Edge case:** permissão negada, operação já realizada

Exemplo: `UpdateProductPriceFunctionTest`
- ✓ Atualiza preço com sucesso
- ✗ Preço negativo → erro
- ✗ Produto não existe → erro

Exemplo: `GenerateShareCodeFunctionTest`
- ✓ Gera novo código
- ✗ Lista não existe → erro
- ✗ Código já existe → retorna existente

**Lint/Detekt/Cobertura:**
- `./gradlew lintDebug` — sem findings
- `./gradlew detekt` — sem findings
- `./gradlew testDebug` — 75 testes total (60 anteriores + 15 novos) ✓
- `./gradlew koverVerify` — cobertura >85%

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| UpdateProductPrice causa inconsistência de budget | Baixa | Média | Validação de preço ≥ 0, testes com budget |
| DuplicateProduct cria loop infinito | Baixa | Baixa | ID gerado via timestamp+hash, não referencial |
| ReopenShoppingList sem validação de status | Média | Média | Verificar status antes, testes de estado |
| ListListMembers expõe informação sensível | Baixa | Média | Apenas membros conhecem lista, segurança por Firestore rules |
| GenerateShareCode sem rate limiting | Baixa | Baixa | Geração local, sem throttle necessário |

## 9. Perguntas em Aberto (bloqueantes)

- [ ] Nenhuma.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (`ProductsUseCase.kt`, `ShoppingReopenUseCase.kt`, etc.).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `additive` (5 novas funções).
- [x] Novo toggle definido: `feat_app_functions_tier3`.
- [x] Sem novo cache — reutiliza use cases/repositórios existentes.
- [x] Perguntas em aberto exauridas.
