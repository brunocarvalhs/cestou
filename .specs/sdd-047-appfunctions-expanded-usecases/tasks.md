# Task: Expansão de AppFunctions com Novos Use Cases para Agentes de IA

- **Slug:** appfunctions-expanded-usecases
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-08
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação

A entrega sdd-046 (AppFunctions básico) expôs apenas 3 funções principais (criar lista, adicionar item, resumo). Para potencializar a integração com Gemini e assistentes de IA, é necessário expandir a superfície de API com mais operações do Cestou (marcar como comprado, remover item, obter produtos, buscar categorias, atualizar orçamento, etc.).

Referência: Demanda do usuário em 2026-11-08 para "analisar funcionalidades e criar novos usecases para que os agentes de IA possam usar as funcionalidades do app".

Rastreabilidade:
- Use cases existentes: `feature/products/domain/usecase/ProductsUseCase.kt`
- Repositórios: `core/domain/repository/ShoppingRepository.kt`
- AppFunctions existentes: `core/appfunctions/functions/` (sdd-046)

## 2. Escopo

- **Inclui:**
  - 7 novos AppFunctions para cobrir fluxos principais:
    - `MarkProductPurchasedFunction`: marcar item como comprado
    - `RemoveProductFunction`: remover item da lista
    - `UpdateShoppingBudgetFunction`: atualizar orçamento da lista
    - `GetShoppingProductsFunction`: listar produtos com status
    - `GetCategorySpendingFunction`: gastos por categoria
    - `DeleteShoppingListFunction`: deletar lista
    - `JoinShoppingListFunction`: entrar em lista compartilhada via código
  - Integração com Analytics (`AnalyticsTracker`)
  - Integração com Crashlytics para captura de erros
  - Testes unitários para cada nova função (3 cenários cada)
  - Feature flag `feat_app_functions_extended` (default: on)

- **Exclui:**
  - Alteração em chat interno
  - Suporte a Wear OS específico para AppFunctions
  - Sincronização offline de AppFunctions

## 3. Impacto Arquitetural

**Módulos afetados:**
- `core:appfunctions` — novas funções (functions/)
- `core:remote-config` — nova flag: `APP_FUNCTIONS_EXTENDED_ENABLED`
- `core:analytics` — eventos já existem (`APP_FUNCTION_INVOKED`)
- Nenhuma alteração em `feature:products`, `feature:shopping` (apenas consume via injeção)

**Padrão aplicado:** Clean Architecture + AppFunctions (igual ao sdd-046)

**Novos arquivos:**
```
core/appfunctions/src/main/java/br/com/brunocarvalhs/howmuch/core/appfunctions/
├── functions/
│   ├── MarkProductPurchasedFunction.kt
│   ├── RemoveProductFunction.kt
│   ├── UpdateShoppingBudgetFunction.kt
│   ├── GetShoppingProductsFunction.kt
│   ├── GetCategorySpendingFunction.kt
│   ├── DeleteShoppingListFunction.kt
│   └── JoinShoppingListFunction.kt
core/appfunctions/src/test/java/.../
├── functions/
│   ├── MarkProductPurchasedFunctionTest.kt
│   ├── RemoveProductFunctionTest.kt
│   ├── UpdateShoppingBudgetFunctionTest.kt
│   ├── GetShoppingProductsFunctionTest.kt
│   ├── GetCategorySpendingFunctionTest.kt
│   ├── DeleteShoppingListFunctionTest.kt
│   └── JoinShoppingListFunctionTest.kt
```

**Diagrama de fluxo:**

```
┌────────────────────────────────────────────────┐
│ Sistema de IA / Gemini (Android 16+)           │
└────────────────────────────────────────────────┘
                       ↓
┌────────────────────────────────────────────────┐
│ AppFunctionDiscovery                            │
└────────────────────────────────────────────────┘
                       ↓
┌────────────────────────────────────────────────┐
│ 10 AppFunctions (sdd-046 + sdd-047)            │
├────────────────────────────────────────────────┤
│ CREATE (1):  CreateShoppingListFunction        │
│              CreateProductFromImageFunction    │
│              JoinShoppingListFunction          │
├────────────────────────────────────────────────┤
│ READ (3):    GetShoppingListSummaryFunction    │
│              GetShoppingProductsFunction       │
│              GetCategorySpendingFunction       │
├────────────────────────────────────────────────┤
│ UPDATE (2):  UpdateShoppingBudgetFunction      │
│              MarkProductPurchasedFunction      │
├────────────────────────────────────────────────┤
│ DELETE (2):  RemoveProductFunction             │
│              DeleteShoppingListFunction        │
└────────────────────────────────────────────────┘
                       ↓
┌────────────────────────────────────────────────┐
│ CestouAppFunctionsService                      │
├────────────────────────────────────────────────┤
│ + isEnabled() → FeatureFlagService             │
│ + trackFunctionInvoked() → AnalyticsTracker    │
│ + recordError() → FirebaseCrashlytics          │
└────────────────────────────────────────────────┘
                       ↓
┌──────────────┬──────────────┬──────────────────┐
│              │              │                  │
▼              ▼              ▼                  ▼
UseCase   Repository    Domain Model   Crashlytics
```

**Injeção via Hilt:**
- Cada função recebe: `ShoppingRepository`, `ProductsUseCase`, `AnalyticsTracker`, `FirebaseCrashlytics`
- `CestouAppFunctionsService` centraliza feature flag + logging

## 4. Contratos e Interfaces

### AppFunctions Expandidas

**MarkProductPurchasedFunction:**
```
suspend fun markProductPurchased(
    shoppingId: String,
    productId: String,
    isPurchased: Boolean  // toggle
): String  // status mensagem
```

**RemoveProductFunction:**
```
suspend fun removeProduct(
    shoppingId: String,
    productId: String
): String  // status mensagem
```

**UpdateShoppingBudgetFunction:**
```
suspend fun updateShoppingBudget(
    shoppingId: String,
    newBudget: Float
): String  // updated budget + remaining
```

**GetShoppingProductsFunction:**
```
suspend fun getShoppingProducts(
    shoppingId: String,
    filterPurchased: Boolean? = null  // null = all
): String  // JSON array de produtos: id,name,qty,price,status
```

**GetCategorySpendingFunction:**
```
suspend fun getCategorySpending(
    shoppingId: String
): String  // JSON: { "Alimentos": 50.00, "Higiene": 30.00, ... }
```

**DeleteShoppingListFunction:**
```
suspend fun deleteShoppingList(
    shoppingId: String
): String  // "Deleted successfully" ou erro
```

**JoinShoppingListFunction:**
```
suspend fun joinShoppingList(
    shortCode: String
): String  // ID da lista ou erro "Invalid code"
```

### Alteração em RemoteConfigKeys.kt

```kotlin
const val APP_FUNCTIONS_EXTENDED_ENABLED = "feat_app_functions_extended"
```

Todas funções marcadas como `public` justificadas: exposição necessária para descoberta por agentes de IA via AppFunctions.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Ações do Cestou requerem interface gráfica | Ações executáveis via voz/Gemini sem abrir UI (7 novos flows) | Arquitetura de Assistentes Android |
| 2 | Apenas proprietário da lista pode alterar | AppFunctions respeitam roles/permissões do usuário autenticado | `ShoppingRepository.kt:1-37` |
| 3 | Orçamento é opcional por lista | AppFunctions permitem atualizar dinamicamente | `Shopping.kt:6-35` |
| 4 | Produtos organizados por categoria | AppFunctions expõem breakdown de gasto por categoria | `Product.kt:7-27` |

## 6. Critérios de Aceitação

**Performance:**
- Cada AppFunction executa em <200ms (mesmo que sdd-046)
- Sem alocações desnecessárias
- Sem I/O duplicado (cache de Flow via `first()` já respeitado)
- Sem recomposições (AppFunctions não são Composables)

**Feature Toggle:**
| Toggle | Default | Fonte | OFF | ON |
|---|---|---|---|---|
| `feat_app_functions` | true | RemoteConfig | Primeiras 3 funções desabilitadas (sdd-046) | ✓ |
| `feat_app_functions_extended` | true | RemoteConfig | Novas 7 funções desabilitadas | ✓ |

**Observabilidade:**
- Evento: `app_function_invoked`
- Parâmetros: `action_id` (nome da função), `success`, `reason` (se erro)
- Logs estruturados: `crashlytics.log()` com contexto completo
- Exceções brutas: `crashlytics.recordException()`

**Compatibilidade:**
- Android 16+ (API 36+) — aprovado no sdd-046
- Sem breaking changes em `core:domain` ou `core:appfunctions`

## 7. Plano de Testes

**Unitários (7 funções × 3 cenários = 21 testes):**

Cada função testa:
1. **Happy path:** sucesso com dados válidos
2. **Erro de validação:** lista não existe, product não existe, budget inválido, etc.
3. **Erro de permissão/acesso:** usuário sem permissão (se aplicável)

Exemplo: `MarkProductPurchasedFunctionTest`:
- ✓ Marca item como comprado
- ✗ Produto não existe → erro
- ✗ Lista não existe → erro

**Instrumentados / ADB:**
- Validação de descoberta: `adb shell appfunctions list`
- Validação de execução: `adb shell appfunctions invoke mark_product_purchased --string ...`

**Lint/Detekt/Cobertura:**
- `./gradlew lintDebug` — sem findings
- `./gradlew detekt` — sem findings  
- `./gradlew testDebug` — 21 testes ✓
- `./gradlew koverVerify` — cobertura >85% nos novos arquivos

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Permissões de acesso não respeitadas em AppFunctions | Baixa | Alta | Validação de autenticação do usuário, testes de acesso denegado |
| AppFunctions não descobertos pelo Gemini (API alpha) | Média | Média | Testes via ADB, fallback para lista hardcoded se necessário |
| Performance: timeout <200ms não atingido | Baixa | Média | Profiling com Firebase Performance, cache em memória |
| Orçamento negativo aceito | Baixa | Média | Validação em `UpdateShoppingBudgetFunction` |

## 9. Perguntas em Aberto (bloqueantes)

- [ ] Nenhuma.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` e `architecture/` quando existir.
- [x] Nenhum código de produção escrito (apenas pseudocódigo/assinaturas/diagramas).
- [x] Seção 6 preenchida de forma substantiva (performance, toggles, observabilidade, compatibilidade).
- [x] Mudança em símbolo público reflete `additive` em "Impacto público" (7 novas funções).
- [x] Novo toggle definido: `feat_app_functions_extended`, observabilidade planejada.
- [x] Sem novo cache — reutiliza use cases/repositórios existentes.
- [x] Perguntas em aberto exauridas.
