# Task: Dynamic Remote Config Strategy para Evolução e Manutenção

- **Slug:** dynamic-remote-config-evolution-maintenance
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-08
- **Status:** ready
- **Versão-alvo:** 2.3.0+
- **Tipo de mudança:** feature (infraestrutura de configuração)
- **Impacto público (módulo):** additive (sem breaking changes)

## 1. Contexto e Motivação

Após reorganizar RemoteConfigKeys.kt com 42 toggles + 12 variáveis (sdd-050), precisamos de uma **estratégia operacional** para:

1. **Deploy das configs no Firebase**: Versionamento, staging, canary rollout
2. **Monitoramento**: Impacto de cada config em crashes, performance, adoção
3. **A/B Testing**: Validar novas features com subsets de usuários antes de 100%
4. **Evolução ágil**: Adicionar/remover configs sem deploy de app
5. **Manutenção**: Responder a bugs/crashes desligando toggles em minutos

**Problema:** 42 toggles espalhados não têm governance, monitoramento nem strategy de rollout.
Risco: Um toggle desligado por erro atinge 100% dos usuários simultâneamente.

Demanda: "sobe essas feature flags no firebase remote config, e crie outro plano para rodar para configurações que o app pode ser dinamico" — criar infraestrutura, processo e observabilidade.

Rastreabilidade:
- Toggles base: `core/remote-config/RemoteConfigKeys.kt:1-200` (sdd-050)
- Consumer pattern: `FeatureFlagService.isEnabled(key)` em qualquer módulo
- Analytics endpoint: `core:analytics` (Firebase Analytics, Crashlytics)

## 2. Escopo

- **Inclui:**
  - Mapeamento de 42 toggles + 12 variáveis → Firebase Remote Config
  - Versionamento de configs (v1.0, v1.1, etc) com Git tracking
  - Rollout strategy: default (100%) | canary (5% → 25% → 50% → 100%) | staged (regional)
  - Dashboard de monitoramento (GA4 Explorations + Crashlytics)
  - A/B testing framework (AB_TEST_* toggles com variant routing)
  - Fallback defaults compilados em código (zero crash se RCC offline)
  - Cache strategy: fetch-on-startup, 12h TTL, sync when app foreground
  - Template para novas configs (docs + validation schema)

- **Exclui:**
  - Alteração de comportamento de app (configs já existem em sdd-050)
  - CI/CD pipeline (fora do escopo; docs apenas)
  - Real-time sync (<5s); aceitamos 12h TTL para stability
  - Multi-region routing de dados (apenas regional UI/analytics)

## 3. Impacto Arquitetural

**Módulos afetados:**
- `core:remote-config` (RemoteConfigService, sync logic)
- `core:analytics` (GA4 event tracking para config changes)
- CI/CD: shell scripts para uploading configs
- Monitoring: GA4 + Fireabse Console dashboard

**Camadas:**
- Data: RemoteConfigService (fetch, cache, local defaults)
- Domain: FeatureFlagService (read-only interface)
- Presentation: FeatureFlagService injected em Composables/ViewModels

**Diagrama de Fluxo (Nova):**

```
┌─────────────────────────────────────────────────────────────┐
│ Firebase Remote Config Console                              │
│ (42 flags + 12 vars, organized by category)                 │
│ ├── Rollout Conditions (canary, staged, A/B)                │
│ └── Version: 1.0.0 (via Git tag)                            │
└─────────────────────────────────────────────────────────────┘
                           ↓
         ┌─────────────────────────────────────────┐
         │ App Startup Flow                        │
         ├─────────────────────────────────────────┤
         │ 1. Load local defaults (RemoteConfigKeys)│
         │ 2. Fetch from Firebase (async, timeout) │
         │ 3. Cache to DataStore (12h TTL)         │
         │ 4. Inject FeatureFlagService            │
         └─────────────────────────────────────────┘
                           ↓
         ┌─────────────────────────────────────────┐
         │ Runtime: FeatureFlagService.isEnabled() │
         │ Returns: Firebase value OR local default│
         └─────────────────────────────────────────┘
                           ↓
         ┌─────────────────────────────────────────┐
         │ Analytics + Monitoring                  │
         ├─────────────────────────────────────────┤
         │ GA4: config_value_changed event         │
         │ Crashlytics: config version tag         │
         │ Dashboard: adoption curve per config    │
         └─────────────────────────────────────────┘
```

---

## 4. Contratos e Interfaces

### Firebase Remote Config Structure

```json
{
  "parameters": {
    "feat_ai_openrouter_enabled": {
      "description": "OpenRouter LLM provider",
      "valueType": "BOOLEAN",
      "defaultValue": {
        "value": "true"
      },
      "conditionalValues": {
        "canary_5pct": {
          "condition": "device.os_version >= '28' && random_percent <= 5",
          "value": "true"
        }
      }
    },
    "feat_app_functions_search_product_enabled": {
      "description": "Search product by name/category",
      "valueType": "BOOLEAN",
      "defaultValue": { "value": "true" }
    },
    "ai_max_tokens": {
      "description": "Max tokens for AI response",
      "valueType": "NUMBER",
      "defaultValue": { "value": "1024" }
    },
    "budget_alert_threshold_percent": {
      "description": "Alert when % of budget spent",
      "valueType": "NUMBER",
      "defaultValue": { "value": "80" }
    }
  },
  "version": {
    "versionNumber": "1.0.0",
    "updateTime": "2026-11-08T10:00:00Z",
    "updateOrigin": "ADMIN_SDK_JAVA",
    "updateUser": "ci-pipeline@Cestou.iam.gserviceaccount.com",
    "description": "sdd-050 toggles reorganization + A/B testing setup"
  }
}
```

### Rollout Strategy Conditions

```kotlin
// Canary: gradual rollout (5% → 25% → 50% → 100%)
"feat_app_functions_search_product_enabled" {
  conditions = {
    "Canary Wave 1 (5%)": {
      targeting = "version >= '2.3.0' && random_percent <= 5",
      value = "true"
    },
    "Canary Wave 2 (25%)": {
      targeting = "version >= '2.3.0' && created_date <= 2026-10-01 && random_percent <= 25",
      value = "true"
    },
    "Staged Rollout (50%)": {
      targeting = "version >= '2.3.0' && country in ['BR', 'US'] && random_percent <= 50",
      value = "true"
    },
    "Full Rollout (100%)": {
      targeting = "version >= '2.3.0'",
      value = "true"
    }
  }
}

// A/B Test: variant assignment
"ab_test_ui_layout_enabled" {
  conditions = {
    "Variant A (Control)": {
      targeting = "user_id % 2 == 0",  // 50% of users
      value = "false"  // Original layout
    },
    "Variant B (New)": {
      targeting = "user_id % 2 == 1",  // 50% of users
      value = "true"   // New layout
    }
  }
}
```

### Local Defaults (Compiled Fallback)

```kotlin
// RemoteConfigKeys.kt - fallback wenn Firebase offline
object RemoteConfigDefaults {
    // AI Features
    const val AI_OPENROUTER_ENABLED_DEFAULT = true
    const val AI_GEMINI_ENABLED_DEFAULT = true
    const val AI_MAX_TOKENS_DEFAULT = 1024
    const val AI_TEMPERATURE_DEFAULT = "0.7"
    
    // AppFunctions - all default ON for resilience
    const val APP_FUNCTIONS_BASE_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_SEARCH_PRODUCT_ENABLED_DEFAULT = true
    // ... 20 more
    
    // Analytics
    const val ANALYTICS_MAX_EVENTS_PER_SESSION_DEFAULT = 50
    const val ANALYTICS_PERFORMANCE_SAMPLING_RATE_DEFAULT = "0.1"
}
```

---

## 5. Regras de Negócio Aplicáveis

| # | Regra | Comportamento Novo | Origem |
|---|---|---|---|
| 1 | Offline resilience | App funciona offline com compiled defaults por 24h | `FeatureFlagService.kt` |
| 2 | Canary safety | Apenas 5% → 25% → 50% → 100%, nunca 0% → 100% | Rollout SOP |
| 3 | A/B variant stability | user_id % 2 permanece fixo (user sempre vê mesmo variant) | Analytics best practice |
| 4 | Config TTL | Fetch no startup + 12h cache, sync quando foreground | `RemoteConfigService.kt` |
| 5 | Crash budget | Se config causa crash >0.1%, Auto-revert no Firebase | SOP incident |
| 6 | Monitoring requirement | Todo novo toggle = GA4 event + Crashlytics tag | sdd-051 §6 |

---

## 6. Critérios de Aceitação

### Deployment & Versioning

- ✓ Todos os 42 toggles + 12 variáveis em Firebase Remote Config
- ✓ Versionamento: Git tag `remote-config/v1.0.0` alinhado com app version
- ✓ Canary rollout testável: 5% → 25% → 50% → 100% conditions
- ✓ Fallback defaults compilados (zero crash offline)

### Monitoring & Observability

- ✓ GA4 Exploration dashboard: "Config Adoption Curve" (per toggle)
- ✓ Crashlytics tag: `remote_config_version` em toda exceção
- ✓ Event `config_value_changed` logged quando toggle muda
- ✓ Alert se crash rate > 0.1% após config push

### A/B Testing

- ✓ Template: `ab_test_*` toggles com variant assignment via user_id % N
- ✓ GA4 event `ab_variant_assigned` with `variant_id`, `user_cohort`
- ✓ Docs: como iterar A/B test (2 weeks typical)

### Cache & Sync Strategy

- ✓ Fetch on app start (async, 5s timeout)
- ✓ Cache to DataStore (12h TTL)
- ✓ Sync when foreground (if stale)
- ✓ Local defaults used if fetch fails or offline

### Template & Governance

- ✓ Template para novo toggle (docs + schema validation)
- ✓ Checklist: GA4 event, Crashlytics tag, rollout plan
- ✓ Code review required antes de push ao Firebase

---

## 7. Plano de Testes

**Unitários:**
- Mock Firebase Remote Config fetch (success, timeout, offline)
- Verify local defaults loaded when network fails
- Verify cache TTL respected (12h expiry)
- Verify DataStore persistence across app restarts

**Instrumentados:**
- Push config change to Firebase staging
- Verify app fetches and applies within 5s
- Verify GA4 event logged with config key + old/new values
- Verify Crashlytics tag includes config version

**Manual:**
- Start app offline → verify defaults loaded
- Change config in Firebase → wait 5s → verify app reloads
- Trigger crash with config ON → verify Crashlytics tag has config version
- A/B test: two devices with user_id % 2 different → verify different variants

**Lint/Detekt/Coverage:**
- `./gradlew lintDebug` — zero findings
- `./gradlew detekt` — zero findings
- `./gradlew testDebug` — new tests for fetch, cache, defaults

---

## 8. Dependências e Riscos

| Risco | Prob | Impacto | Mitigação |
|---|---|---|---|
| Firebase RCC push breaks 100% users | Baixa | Alto | Canary rollout (5% first), monitoring |
| Config value causes ANR/crash | Média | Alto | Crash budget monitoring, auto-revert SOP |
| DataStore corruption loses cache | Baixa | Média | Compiled defaults fallback |
| 12h cache too stale | Baixa | Baixo | Acceptable; can force sync on demand |
| A/B variant instability (user sees different variant) | Baixa | Médio | Fixed seed per user_id (user_id % 2) |

---

## 9. Perguntas em Aberto (bloqueantes)

- [ ] Nenhuma.

---

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (`RemoteConfigKeys.kt:1-200`, `FeatureFlagService`)
- [x] Nenhum código de produção escrito (apenas pseudocódigo, JSON configs, diagramas)
- [x] Seção 6 preenchida de forma substantiva
- [x] Mudança em contrato público: `additive` (sem breaking)
- [x] Firebase Remote Config mapping especificado (42 toggles + 12 vars)
- [x] Rollout strategy (canary, staged, A/B) documentada
- [x] Monitoramento (GA4 + Crashlytics) planejado
- [x] Cache strategy (12h TTL, DataStore) justificada
- [x] Fallback defaults em código para offline resilience
- [x] Perguntas em aberto exauridas

---

## Apêndice A: Rollout SOP (Standard Operating Procedure)

### Processo de Deploy (New Config ou Feature)

```
1. DEV: Escrever config em RemoteConfigKeys.kt (local default)
2. PR: Code review + tests
3. STAGING: Merge para develop → app version x.y.z
4. FIREBASE STAGING: Push config com default value (0% users)
5. QA: Testar com Staging config
6. PROD: When app version published:
   a. Week 1: Canary 5% (random_percent <= 5)
   b. Week 2: Canary 25% (active users >7 days)
   c. Week 3: Staged 50% (BR + US only)
   d. Week 4: Full 100% (all users)
7. MONITORING: GA4 adoption curve, Crashlytics crash rate
8. ROLLBACK: If crash rate > 0.1%, set value to default OR disable toggle
```

### Incident Response (Config Causes Crash)

```
1. Alert: Crashlytics reports crash spike + config_version tag
2. TRIAGE: Link crash to specific config change
3. ACTION (30 min max):
   a. Firebase Console: Toggle value → false OR revert to previous
   b. Update Git tag (e.g., v1.0.1-hotfix)
   c. Post-mortem: Why? Rollout too fast?
4. RESOLUTION: Deploy fixed app version when ready
```

---

## Apêndice B: GA4 Exploration Queries

### Query 1: Config Adoption Curve (per toggle)

```
Event: config_value_changed
Dimensions: event_parameter(config_key), event_date, country
Metrics: user_count, active_users
Timeline: Last 30 days
Breakdown: by config_key (feat_app_functions_search_product_enabled, etc)
Result: Line chart showing % of active users who saw value=true over time
```

### Query 2: A/B Test Performance

```
Event: screen_view
Dimensions: event_parameter(ab_variant_id), screen_name
Metrics: engagement_time_msec, user_count, event_count
Breakdown: by ab_variant_id (variant_a_control, variant_b_new)
Result: Compare engagement metrics between variants
```

### Query 3: Config Impact on Crashes

```
Intersection:
  - Crashlytics: crashes WITH tag remote_config_version=1.0.0
  - GA4: users active with config_key=feat_*
Metrics: crash_rate, affected_user_count
Result: Identify if config correlates with crash spike
```

---

## Apêndice C: Template para Nova Config

Quando adicionar novo toggle/variável, usar:

```kotlin
// 1. RemoteConfigKeys.kt
object FeatureFlagKeys {
    // CATEGORY: <Funcionalidade>
    /**
     * <Breve descrição>.
     * 
     * **Purpose:** <Por que existe>
     * **Dependencies:** <outros toggles necessários>
     * **Default:** on/off
     * **Rollout:** Canary → Staged → 100% (2 weeks typical)
     * **Monitoring:** GA4 event + Crashlytics tag
     */
    const val NEW_FEATURE_ENABLED = "feat_new_feature"
}

object RemoteConfigDefaults {
    const val NEW_FEATURE_ENABLED_DEFAULT = true  // Offline default
}

// 2. Firebase Console
// Add parameter:
{
  "feat_new_feature": {
    "description": "...",
    "valueType": "BOOLEAN",
    "defaultValue": { "value": "true" },
    "conditionalValues": {
      "canary_5pct": {
        "condition": "random_percent <= 5",
        "value": "true"
      }
    }
  }
}

// 3. Code: Usage
if (featureFlagService.isEnabled(FeatureFlagKeys.NEW_FEATURE_ENABLED)) {
    // New behavior
} else {
    // Fallback
}

// 4. Analytics: Log config change
analyticsTracker.trackEvent("config_value_changed", mapOf(
    "config_key" to "feat_new_feature",
    "old_value" to "false",
    "new_value" to "true",
    "source" to "remote_config"
))

// 5. Checklist:
// [ ] Documentado em RemoteConfigKeys.kt
// [ ] Default compilado em RemoteConfigDefaults
// [ ] Firebase config adicionado (staging primeiro)
// [ ] GA4 event para mudança
// [ ] Crashlytics tag adicionado
// [ ] Canary rollout plan escrito (e.g., 5% → 25% → 100%)
// [ ] Code review aprovado
```
