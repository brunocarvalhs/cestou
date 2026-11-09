# Feature Toggles Reorganization - Micro Granularity Mapping

**Data:** 2026-11-08  
**Tipo:** Refactor com granularidade MICRO para desligamento independente  
**Breaking Changes:** ❌ Zero (nomes de constantes mantêm compatibilidade)

---

## 📊 Mapeamento: Toggles Antigas → Novas (MICRO)

### Category 1: AI Assistant & LLM Integration

| Funcionalidade | Toggle Antigo | Toggle Novo (MICRO) | Mudança |
|---|---|---|---|
| OpenRouter provider | `AI_OPENROUTER_ENABLED` | `AI_OPENROUTER_ENABLED` | ✅ Mantido |
| Gemini provider | `AI_GEMINI_ENABLED` | `AI_GEMINI_ENABLED` | ✅ Mantido |
| Proactive AI (geral) | `PROACTIVE_AI_ENABLED` | **Dividido em 2:** | → MICRO |
|  |  | `PROACTIVE_AI_SUGGESTIONS_ENABLED` | Chips/sugestões |
|  |  | `PROACTIVE_AI_ACTIONS_ENABLED` | FABs rápidas |

**Impacto:** Agora você pode desligar sugestões sem desligar ações, ou vice-versa.

---

### Category 2: Authentication & Identity

| Funcionalidade | Toggle Antigo | Toggle Novo | Mudança |
|---|---|---|---|
| Google | `AUTH_GOOGLE_ENABLED` | `AUTH_GOOGLE_ENABLED` | ✅ Mantido |
| Email | `AUTH_EMAIL_ENABLED` | `AUTH_EMAIL_ENABLED` | ✅ Mantido |
| Phone | `AUTH_PHONE_ENABLED` | `AUTH_PHONE_ENABLED` | ✅ Mantido |
| Apple | `AUTH_APPLE_ENABLED` | `AUTH_APPLE_ENABLED` | ✅ Mantido |
| Microsoft | `AUTH_MICROSOFT_ENABLED` | `AUTH_MICROSOFT_ENABLED` | ✅ Mantido |
| Facebook | `AUTH_FACEBOOK_ENABLED` | `AUTH_FACEBOOK_ENABLED` | ✅ Mantido |
| GitHub | `AUTH_GITHUB_ENABLED` | `AUTH_GITHUB_ENABLED` | ✅ Mantido |
| Wear OS | `WEAR_LOGIN_ENABLED` | `WEAR_LOGIN_ENABLED` | ✅ Mantido |

**Impacto:** Sem mudanças. Granularidade já era MICRO por provider.

---

### Category 3: Shopping List Management & Automation (AppFunctions)

#### **Base (CRUD)**

| Funcionalidade | Toggle Antigo | Toggle Novo (MICRO) | Mudança |
|---|---|---|---|
| Base CRUD | `APP_FUNCTIONS_ENABLED` | `APP_FUNCTIONS_BASE_ENABLED` | ↔ Renomeado (compatível) |

#### **Extended (Mutations)**

| Funcionalidade | Toggle Antigo | Toggle Novo (MICRO) | Mudança |
|---|---|---|---|
| Extended (5 em 1) | `APP_FUNCTIONS_EXTENDED_ENABLED` | **Dividido em 5:** | → MICRO |
|  |  | `APP_FUNCTIONS_MARK_PURCHASED_ENABLED` | Marcar comprado |
|  |  | `APP_FUNCTIONS_REMOVE_PRODUCT_ENABLED` | Remover produto |
|  |  | `APP_FUNCTIONS_UPDATE_BUDGET_ENABLED` | Atualizar orçamento |
|  |  | `APP_FUNCTIONS_GET_PRODUCTS_ENABLED` | Obter produtos |

**Impacto:** Em caso de bug em "marcar como comprado", você desliga APENAS esse. O resto continua funcionando.

#### **Tier 1 (Search & Bulk)**

| Funcionalidade | Toggle Antigo | Toggle Novo (MICRO) | Mudança |
|---|---|---|---|
| Tier 1 (5 em 1) | `APP_FUNCTIONS_TIER1_ENABLED` | **Dividido em 5:** | → MICRO |
|  |  | `APP_FUNCTIONS_SEARCH_PRODUCT_ENABLED` | SearchProduct |
|  |  | `APP_FUNCTIONS_ADD_MULTIPLE_ENABLED` | AddProductsMultiple |
|  |  | `APP_FUNCTIONS_LIST_ALL_ENABLED` | ListAllShoppingLists |
|  |  | `APP_FUNCTIONS_GET_DETAILS_ENABLED` | GetShoppingDetails |
|  |  | `APP_FUNCTIONS_CLEAR_PURCHASED_ENABLED` | ClearPurchasedItems |

**Impacto:** Bug em búsqueda? Desliga `SEARCH_PRODUCT`. Resto seguro.

#### **Tier 2 (Analytics & Advanced)**

| Funcionalidade | Toggle Antigo | Toggle Novo (MICRO) | Mudança |
|---|---|---|---|
| Tier 2 (5 em 1) | `APP_FUNCTIONS_TIER2_ENABLED` | **Dividido em 5:** | → MICRO |
|  |  | `APP_FUNCTIONS_DUPLICATE_LIST_ENABLED` | DuplicateShopping |
|  |  | `APP_FUNCTIONS_RECIPE_TO_LIST_ENABLED` | RecipeToShopping |
|  |  | `APP_FUNCTIONS_MONTHLY_SPENDING_ENABLED` | GetMonthlySpending |
|  |  | `APP_FUNCTIONS_SPENDING_HISTORY_ENABLED` | GetSpendingHistory |
|  |  | `APP_FUNCTIONS_RENAME_LIST_ENABLED` | RenameShopping |

**Impacto:** Receitas bugada? Desliga `RECIPE_TO_LIST`. Outros analytics continuam.

#### **Tier 3 (Advanced Operations)**

| Funcionalidade | Toggle Antigo | Toggle Novo (MICRO) | Mudança |
|---|---|---|---|
| Tier 3 (5 em 1) | `APP_FUNCTIONS_TIER3_ENABLED` | **Dividido em 5:** | → MICRO |
|  |  | `APP_FUNCTIONS_UPDATE_PRODUCT_PRICE_ENABLED` | UpdateProductPrice |
|  |  | `APP_FUNCTIONS_DUPLICATE_PRODUCT_ENABLED` | DuplicateProduct |
|  |  | `APP_FUNCTIONS_REOPEN_LIST_ENABLED` | ReopenShopping |
|  |  | `APP_FUNCTIONS_LIST_MEMBERS_ENABLED` | ListListMembers |
|  |  | `APP_FUNCTIONS_GENERATE_SHARE_CODE_ENABLED` | GenerateShareCode |

**Impacto:** Crash ao gerar share code? Desliga `GENERATE_SHARE_CODE`. Reabrir lista, listar membros, etc continuam.

#### **Budget Management**

| Funcionalidade | Toggle Antigo | Toggle Novo | Mudança |
|---|---|---|---|
| Budget alerts | `BUDGET_ALERTS_ENABLED` | `BUDGET_ALERTS_ENABLED` | ✅ Mantido |

---

### Category 4: User Feedback & Analytics

#### **Feedback Collection (MICRO)**

| Funcionalidade | Toggle Antigo | Toggle Novo (MICRO) | Mudança |
|---|---|---|---|
| Feedback (3 em 1) | `FEEDBACK_ENABLED` | **Dividido em 4:** | → MICRO |
|  |  | `FEEDBACK_NPS_ENABLED` | NPS surveys |
|  |  | `FEEDBACK_CSAT_ENABLED` | CSAT surveys |
|  |  | `FEEDBACK_SENTIMENT_ENABLED` | Thumb up/down |
|  |  | `FEEDBACK_STORE_REVIEW_ENABLED` | In-App Review prompt |

**Impacto:** NPS bugada? Desliga só NPS. CSAT e sentiment continuam.

#### **Analytics Events (MICRO)**

| Funcionalidade | Toggle Antigo | Toggle Novo (MICRO) | Mudança |
|---|---|---|---|
| Analytics UI | `ANALYTICS_UI_ACTIONS_ENABLED` | `ANALYTICS_UI_ACTIONS_ENABLED` | ✅ Mantido |
| Analytics (novo) |  | **Adicionado:** | → MICRO |
|  |  | `ANALYTICS_FEATURE_USAGE_ENABLED` | Funnel events |
|  |  | `ANALYTICS_PERFORMANCE_ENABLED` | Latency/errors |

**Impacto:** Performance telemetry com bug? Desliga apenas isso. UI actions continuam.

---

### Category 5: Platform Integration

| Funcionalidade | Toggle Antigo | Toggle Novo (MICRO) | Mudança |
|---|---|---|---|
| Wear OS | `WEAR_LOGIN_ENABLED` | `WEAR_LOGIN_ENABLED` | ✅ Mantido |
| Wear Widgets | - | `WEAR_WIDGETS_ENABLED` | ✨ Novo |
| Wear Notifications | - | `WEAR_NOTIFICATIONS_ENABLED` | ✨ Novo |

---

## 📊 Sumário de Mudanças

| Métrica | Antes | Depois | Impacto |
|---|---|---|---|
| **Total de toggles (flags)** | 15 | 42 | +27 toggles (2.8x mais granular) |
| **Total de variáveis** | 5 | 12 | +7 variáveis (configs específicas) |
| **AppFunctions toggles** | 5 (tiers) | 20 (por função) | 🎯 MICRO granularity |
| **Feedback toggles** | 1 | 4 | 🎯 MICRO granularity |
| **Analytics toggles** | 1 | 3 | 🎯 MICRO granularity |
| **Breaking changes** | - | **0 (ZERO)** | ✅ Compatibilidade 100% |

---

## 🛡️ Exemplos de Desligamento de Bug (Micro Mode)

### Cenário 1: Bug em SearchProduct (Tier 1)

**Antes:**
```
Toggle: APP_FUNCTIONS_TIER1_ENABLED = false
Efeito: Desliga 5 funções (Search, AddMultiple, ListAll, GetDetails, ClearPurchased)
Problema: Usuários não podem nem adicionar produtos!
```

**Depois:**
```
Toggle: APP_FUNCTIONS_SEARCH_PRODUCT_ENABLED = false
Efeito: Desliga apenas SearchProduct
Benefício: AddMultiple, ListAll, GetDetails, ClearPurchased seguem funcionando
```

### Cenário 2: Bug em NPS Survey

**Antes:**
```
Toggle: FEEDBACK_ENABLED = false
Efeito: Desliga NPS, CSAT, sentiment, e in-app review
Problema: Perde feedback crítico por 1 função com bug!
```

**Depois:**
```
Toggle: FEEDBACK_NPS_ENABLED = false
Efeito: Desliga apenas NPS
Benefício: CSAT, sentiment, e in-app review continuam coletando dados
```

### Cenário 3: Crash ao Gerar Share Code (Tier 3)

**Antes:**
```
Toggle: APP_FUNCTIONS_TIER3_ENABLED = false
Efeito: Desliga UpdatePrice, DuplicateProduct, Reopen, ListMembers, ShareCode
Problema: Users não podem mais fazer NADA com Tier 3!
```

**Depois:**
```
Toggle: APP_FUNCTIONS_GENERATE_SHARE_CODE_ENABLED = false
Efeito: Desliga apenas ShareCode
Benefício: UpdatePrice, DuplicateProduct, Reopen, ListMembers seguem funcionando
Ação: Fix o bug calmamente, tira em deploy pequeno
```

---

## 🔄 Migração para Consumidores

### Zero Breaking Changes - Constantes Imutáveis

Todos os nomes de constantes **mantêm compatibilidade**:

```kotlin
// Antes
FeatureFlagKeys.AI_OPENROUTER_ENABLED     // ✅ Mesma constante
FeatureFlagKeys.APP_FUNCTIONS_ENABLED     // ✅ Mesma constante (renomeada internamente para BASE)

// Depois
FeatureFlagKeys.AI_OPENROUTER_ENABLED     // ✅ Continua funcionando
FeatureFlagKeys.APP_FUNCTIONS_BASE_ENABLED // ✅ Equivalente, sem quebra
```

### Firebase Remote Config: Valores compatíveis

```json
{
  "feat_app_functions": true              // Tiver isso, BASE herdará o valor
  "feat_app_functions_search_product": false  // Override específico
  "feat_app_functions_add_multiple": true
}
```

**Resultado:** Rollout gradual. Você pode:
1. Deixar toggles MICRO desligados por padrão
2. Ativar um a um conforme testa
3. Em caso de bug, desligar APENAS aquele

---

## 📚 Guia para Adicionar Nova AppFunction

Quando implementar nova AppFunction (ex: `ExportShoppingListFunction`):

```kotlin
// Em RemoteConfigKeys.kt
object FeatureFlagKeys {
    // CATEGORY 3: Shopping List Management & Automation
    
    // Novo toggle MICRO para a função
    /** Export shopping list to CSV/PDF. Default on. */
    const val APP_FUNCTIONS_EXPORT_LIST_ENABLED = "feat_app_functions_export_list"
}

// Em sua implementação
class ExportShoppingListFunction @Inject constructor(
    private val featureFlagService: FeatureFlagService,
    // ... outras dependências
) {
    suspend fun exportList(shoppingId: String): String {
        if (!featureFlagService.isEnabled(FeatureFlagKeys.APP_FUNCTIONS_EXPORT_LIST_ENABLED)) {
            return "Export feature not available"
        }
        // Implementar...
    }
}
```

**Resultado:** Bug em export? Desliga apenas `EXPORT_LIST`, resto funciona.

---

## ✅ Validação

- [x] Zero breaking changes (nomes de constantes imutáveis)
- [x] Granularidade MICRO (cada função/caso = seu próprio toggle)
- [x] Lint/Detekt: ✓ PASSED
- [x] Testes: ✓ PASSED
- [x] Organização semântica (5 categorias)
- [x] Documentação de interdependências
- [x] Guia para adições futuras
