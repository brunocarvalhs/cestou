# Task: Estender Remote Config com 20 Novas Configurações

- **Slug:** remote-config-extended-configuration
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-08
- **Status:** ready
- **Versão-alvo:** 2.3.0+
- **Tipo de mudança:** feature (adição de configurações)
- **Impacto público (módulo):** additive (sem breaking changes)

## 1. Contexto e Motivação

**Problema:** Após sdd-051, temos 54 configurações mapeadas. Mas análise de funnel de ativação (79.9% perda entre criar lista e adicionar primeiro produto) + issues operacionais (Gemini Vision timeout, Wear sync instável) revelam oportunidades de tunning dinâmico que economizariam semanas de deploy.

**Demanda:** Adicionar 20 novas configurações em 3 categorias críticas:
1. **API/Network** (7) — Gemini timeout + retry logic (URGENT: photo-scan fallback quebrado)
2. **UX/Onboarding** (8) — A/B test CTAs, sample products, AI chat suggestions (HIGH IMPACT: 79.9% activation loss)
3. **Data/Sync** (5) — Wear sync tuning, cache TTL, conflict resolution (OPERATIONAL)

**Rastreabilidade:**
- RemoteConfigDefaults.kt (sdd-050, §1-5): onde defaults compilados vivem
- RemoteConfigKeys.kt (sdd-050, §1-3): onde constantes são definidas
- Gemini timeout issue: conhecido vivo, needs quick fix without deploy
- Activation funnel: GA4 data mostra 687 usuários, 549 drop no primeiro passo

## 2. Escopo

- **Inclui:**
  - Adicionar 20 novas const em `RemoteConfigKeys.kt` (7 + 8 + 5)
  - Adicionar 20 novos defaults em `RemoteConfigDefaults.kt`
  - Documentação de cada config (KDoc com purpose, type, default, rollout plan)
  - Atualizar `.specs/sdd-051-remote-config-evolution-maintenance/firebase-upload.sh` script com novas configs
  - Atualizar `CONFIG_TEMPLATE.md` com exemplos das novas categorias
  - Nenhum código de consumo (implementação fica para sdd-053+)

- **Exclui:**
  - Implementação de retry logic (será em sdd-053)
  - UI changes baseadas em configs (será em sdd-054+)
  - Testes de comportamento dinâmico (será em sdd-053+)
  - Migração de configs existentes (backward compat: aliases se houver)

## 3. Impacto Arquitetural

**Módulos afetados:**
- `core:remote-config` (RemoteConfigKeys.kt, RemoteConfigDefaults.kt)
- CI/CD: update firebase-upload.sh script

**Camadas:**
- Data: RemoteConfigKeys (constants), RemoteConfigDefaults (fallbacks)
- Future (sdd-053+): Data layer uses these configs

**Diagrama (Sem mudanças de estrutura, apenas expansão):**

```
┌──────────────────────────────────────────────┐
│ RemoteConfigKeys.kt (Expandido: 62 + 20)    │
├──────────────────────────────────────────────┤
│ CATEGORY 1-5 (Antes): 42 toggles + 12 vars  │
│ CATEGORY 6: API/Network (7 new)             │
│ CATEGORY 7: UX/Onboarding (8 new)           │
│ CATEGORY 8: Data/Sync (5 new)               │
└──────────────────────────────────────────────┘
           ↓
┌──────────────────────────────────────────────┐
│ RemoteConfigDefaults.kt (Expandido)         │
├──────────────────────────────────────────────┤
│ 20 novos defaults compilados (offline)      │
└──────────────────────────────────────────────┘
           ↓
┌──────────────────────────────────────────────┐
│ Firebase Remote Config (Staging → Prod)     │
├──────────────────────────────────────────────┤
│ 20 novos parameters com defaults            │
└──────────────────────────────────────────────┘
```

---

## 4. Contratos e Interfaces

### RemoteConfigKeys.kt — 20 Novas Constantes

**CATEGORY 6: API & Network Configuration**

```kotlin
// Network & Timeouts
const val API_REQUEST_TIMEOUT_SECONDS = "api_request_timeout_seconds"

// Retry Strategy (exponential backoff)
const val API_RETRY_MAX_ATTEMPTS = "api_retry_max_attempts"
const val API_RETRY_BACKOFF_MULTIPLIER = "api_retry_backoff_multiplier"

// Gemini Vision Fallback (CRITICAL - currently broken)
const val FEAT_AI_VISION_FALLBACK_ENABLED = "feat_ai_vision_fallback_enabled"
const val AI_VISION_TIMEOUT_SECONDS = "ai_vision_timeout_seconds"
const val PHOTO_SCAN_RETRY_COUNT = "photo_scan_retry_count"
const val AI_VISION_OFFLINE_MODE_ENABLED = "feat_ai_vision_offline_mode_enabled"
```

**CATEGORY 7: UX & Onboarding**

```kotlin
// Onboarding Skip & Tips
const val FEAT_ONBOARDING_SKIP_ENABLED = "feat_onboarding_skip_enabled"
const val FEAT_ONBOARDING_SHOW_TIPS = "feat_onboarding_show_tips"

// Empty State Aggression (for activation funnel)
const val FEAT_EMPTY_STATE_CTA_AGGRESSIVE = "feat_empty_state_cta_aggressive"

// Sample Products (to reduce 79.9% funnel loss)
const val FEAT_AUTO_ADD_SAMPLE_PRODUCTS = "feat_auto_add_sample_products"
const val SAMPLE_PRODUCTS_COUNT = "sample_products_count"

// AI Chat FAB UX (430 open, only 160 send = 62% drop)
const val AI_CHAT_FAB_SHOW_BADGE = "ai_chat_fab_show_badge"
const val FEAT_AI_CHAT_FIRST_MESSAGE_SUGGESTION = "feat_ai_chat_first_message_suggestion"
const val AI_CHAT_SUGGESTION_EXAMPLES = "ai_chat_suggestion_examples"

// Animation & Performance
const val FEAT_ENABLE_LIST_ANIMATIONS = "feat_enable_list_animations"
const val ANIMATION_DURATION_MS = "animation_duration_ms"
```

**CATEGORY 8: Data & Sync Configuration**

```kotlin
// Firestore & Local Cache
const val FIRESTORE_BATCH_SIZE = "firestore_batch_size"
const val LOCAL_CACHE_TTL_MINUTES = "local_cache_ttl_minutes"
const val OFFLINE_MODE_MAX_PENDING_CHANGES = "offline_mode_max_pending_changes"

// Wear OS Sync (currently: 15min intervals, often flaky)
const val WEAR_FULL_SYNC_INTERVAL_MINUTES = "wear_full_sync_interval_minutes"
const val WEAR_DELTA_SYNC_INTERVAL_SECONDS = "wear_delta_sync_interval_seconds"

// Share Code & Collaboration (conflict resolution for shared lists)
const val SHARED_LIST_CONFLICT_RESOLUTION = "shared_list_conflict_resolution"
```

---

### RemoteConfigDefaults.kt — 20 Novos Defaults

```kotlin
// ============================================================================
// CATEGORY 6: API & Network Configuration
// ============================================================================

const val API_REQUEST_TIMEOUT_SECONDS_DEFAULT = 30
const val API_RETRY_MAX_ATTEMPTS_DEFAULT = 3
const val API_RETRY_BACKOFF_MULTIPLIER_DEFAULT = "1.5"
const val FEAT_AI_VISION_FALLBACK_ENABLED_DEFAULT = true  // Can be disabled if broken
const val AI_VISION_TIMEOUT_SECONDS_DEFAULT = 10
const val PHOTO_SCAN_RETRY_COUNT_DEFAULT = 2
const val AI_VISION_OFFLINE_MODE_ENABLED_DEFAULT = false

// ============================================================================
// CATEGORY 7: UX & Onboarding
// ============================================================================

const val FEAT_ONBOARDING_SKIP_ENABLED_DEFAULT = false
const val FEAT_ONBOARDING_SHOW_TIPS_DEFAULT = true
const val FEAT_EMPTY_STATE_CTA_AGGRESSIVE_DEFAULT = false
const val FEAT_AUTO_ADD_SAMPLE_PRODUCTS_DEFAULT = false  // Test via canary first
const val SAMPLE_PRODUCTS_COUNT_DEFAULT = 3
const val AI_CHAT_FAB_SHOW_BADGE_DEFAULT = true
const val FEAT_AI_CHAT_FIRST_MESSAGE_SUGGESTION_DEFAULT = true
const val AI_CHAT_SUGGESTION_EXAMPLES_DEFAULT = "add,search,recipe"

const val FEAT_ENABLE_LIST_ANIMATIONS_DEFAULT = true
const val ANIMATION_DURATION_MS_DEFAULT = 300

// ============================================================================
// CATEGORY 8: Data & Sync Configuration
// ============================================================================

const val FIRESTORE_BATCH_SIZE_DEFAULT = 25
const val LOCAL_CACHE_TTL_MINUTES_DEFAULT = 60
const val OFFLINE_MODE_MAX_PENDING_CHANGES_DEFAULT = 100
const val WEAR_FULL_SYNC_INTERVAL_MINUTES_DEFAULT = 15
const val WEAR_DELTA_SYNC_INTERVAL_SECONDS_DEFAULT = 30
const val SHARED_LIST_CONFLICT_RESOLUTION_DEFAULT = "last_write_wins"
```

---

## 5. Regras de Negócio Aplicáveis

| # | Regra | Comportamento com Novas Configs | Origem |
|---|---|---|---|
| 1 | Gemini Vision timeout | `AI_VISION_TIMEOUT_SECONDS` ajustável (default 10s, pode aumentar se lento) | Known issue: photo-scan fallback |
| 2 | Retry strategy | `API_RETRY_MAX_ATTEMPTS` + `API_RETRY_BACKOFF_MULTIPLIER` controlam exponential backoff sem código | Network resilience |
| 3 | Activation funnel | `FEAT_AUTO_ADD_SAMPLE_PRODUCTS` pode melhorar list-creation→first-product (79.9% loss) | GA4: 687 users, 549 drop |
| 4 | AI Chat FAB adoption | `AI_CHAT_FIRST_MESSAGE_SUGGESTION` mostra exemplos para aumentar 160/430 (37% conversion) | GA4: open→send drop |
| 5 | Wear Sync reliability | `WEAR_FULL_SYNC_INTERVAL_MINUTES` + `WEAR_DELTA_SYNC_INTERVAL_SECONDS` tunable sem deploy | Operational: currently flaky |
| 6 | Offline resilience | `LOCAL_CACHE_TTL_MINUTES` + `OFFLINE_MODE_MAX_PENDING_CHANGES` controla buffer | Offline-first strategy |
| 7 | Shared list conflicts | `SHARED_LIST_CONFLICT_RESOLUTION` resolve race conditions (last_write_wins, etc) | Collaboration feature |

---

## 6. Critérios de Aceitação

### Configurações Adicionadas (20 total)

| Config Name | Type | Default | Purpose | Critical? |
|---|---|---|---|---|
| `api_request_timeout_seconds` | NUMBER | 30 | Timeout para chamadas de API | Medium |
| `api_retry_max_attempts` | NUMBER | 3 | Máx tentativas de retry | Medium |
| `api_retry_backoff_multiplier` | STRING | "1.5" | Exponential backoff multiplier | Medium |
| `feat_ai_vision_fallback_enabled` | BOOLEAN | true | Enable Gemini fallback (or disable if broken) | **CRITICAL** |
| `ai_vision_timeout_seconds` | NUMBER | 10 | Timeout específico para Gemini Vision | **CRITICAL** |
| `photo_scan_retry_count` | NUMBER | 2 | Retries para photo OCR | Medium |
| `feat_ai_vision_offline_mode_enabled` | BOOLEAN | false | Cache vision results offline | Low |
| `feat_onboarding_skip_enabled` | BOOLEAN | false | Allow skip onboarding | Low |
| `feat_onboarding_show_tips` | BOOLEAN | true | Show tips in onboarding | Low |
| `feat_empty_state_cta_aggressive` | BOOLEAN | false | Aggressive CTA in empty state | **HIGH IMPACT** |
| `feat_auto_add_sample_products` | BOOLEAN | false | Pre-populate sample products | **HIGH IMPACT** |
| `sample_products_count` | NUMBER | 3 | How many samples to add | High |
| `ai_chat_fab_show_badge` | BOOLEAN | true | Show notification badge on FAB | High |
| `feat_ai_chat_first_message_suggestion` | BOOLEAN | true | Show suggestions for first message | **HIGH IMPACT** |
| `ai_chat_suggestion_examples` | STRING | "add,search,recipe" | Comma-separated suggestion types | High |
| `feat_enable_list_animations` | BOOLEAN | true | Enable Compose animations | Medium |
| `animation_duration_ms` | NUMBER | 300 | Duration of animations | Medium |
| `firestore_batch_size` | NUMBER | 25 | Batch size for Firestore queries | Medium |
| `local_cache_ttl_minutes` | NUMBER | 60 | TTL for local cache | Medium |
| `offline_mode_max_pending_changes` | NUMBER | 100 | Max local changes before error | Medium |
| `wear_full_sync_interval_minutes` | NUMBER | 15 | Full sync interval (Wear OS) | Medium |
| `wear_delta_sync_interval_seconds` | NUMBER | 30 | Delta sync interval (Wear OS) | **OPERATIONAL** |
| `shared_list_conflict_resolution` | STRING | "last_write_wins" | Conflict resolution strategy | Medium |

### Observabilidade

- GA4 event: `config_value_changed` com todas as 20 novas configs
- Crashlytics tag: `remote_config_version` inclui todas as 20 configs
- Dashboard GA4: "Config Adoption" mostra adoção de cada uma

### Compatibilidade

- ✓ Backward compatible (0 breaking changes)
- ✓ minSdk 28+
- ✓ Wear OS compatible (wear configs testáveis em Wear emulator)
- ✓ Offline defaults compilados em código

---

## 7. Plano de Testes

### Unitários

- ✓ RemoteConfigDefaults possui todas as 20 constantes e valores padrão
- ✓ RemoteConfigKeys possui todas as 20 chaves com KDoc
- ✓ Sem magic strings (todas as chaves vêm de constantes)
- ✓ Sem duplicatas entre RemoteConfigKeys e RemoteConfigDefaults

### Verificações Estáticas

- ✓ `./gradlew lintDebug` — zero findings
- ✓ `./gradlew detekt` — zero findings
- ✓ Nenhuma quebra de compilação

### Verificações Manuais (Staging Firebase)

- [ ] Upload all 20 configs to Staging Firebase
- [ ] Verify defaults match RemoteConfigDefaults.kt
- [ ] Test fetch on Staging (should get values or fallback to compiled defaults)
- [ ] Test offline (should use compiled defaults)

### Não Incluído (Para sdd-053+)

- Testes de comportamento (retry logic, animation timing, etc)
- Testes de integração (Wear sync, Firestore batch)
- A/B testing (implementação de variant assignment)

---

## 8. Dependências e Riscos

| Risco | Prob | Impacto | Mitigação |
|---|---|---|---|
| Firebase schema mismatch entre staging/prod | Baixa | Alto | Review configs antes de upload; use script com validação |
| Defaults desincronizados (RemoteConfigKeys vs RemoteConfigDefaults) | Baixa | Médio | Testes de verificação estática |
| Configs mal nomeadas (magic strings) | Baixa | Médio | Todas as chaves são constantes, sem magic strings |
| Config explosion (muito granular, difícil manter) | Média | Médio | Documentar claramente cada config + rollout SOP |
| Behavior mismatch (código espera config, Firebase tem outra coisa) | Baixa | Alto | Será responsabilidade de sdd-053+ (implementação) |

---

## 9. Perguntas em Aberto (bloqueantes)

- [ ] Nenhuma.

---

## 10. Checklist de Conformidade

- [x] Decisões citam código/origem (sdd-050 RemoteConfigKeys/Defaults, GA4 funnel data)
- [x] Nenhum código de produção (apenas constantes + documentação)
- [x] Seção 6 preenchida de forma substantiva (20 configs tabuladas)
- [x] Sem breaking changes (additive apenas)
- [x] Novo remote-config: 20 toggles/numbers/strings mapeados
- [x] KDoc para cada config (purpose, type, default, rollout plan)
- [x] Perguntas em aberto exauridas

---

## Apêndice A: Como Implementar (sdd-053+)

Após aprovação deste plano:

1. **sdd-053: API/Network Retry Logic**
   - Implementar exponential backoff com `API_RETRY_MAX_ATTEMPTS` + `API_RETRY_BACKOFF_MULTIPLIER`
   - Usar para Gemini Vision + chamadas de API

2. **sdd-054: UX/Onboarding Improvements**
   - Sample products logic com `FEAT_AUTO_ADD_SAMPLE_PRODUCTS`
   - AI Chat suggestions com `AI_CHAT_FIRST_MESSAGE_SUGGESTION`
   - CTA aggression em empty state

3. **sdd-055: Wear OS & Sync Tuning**
   - Delta sync com `WEAR_DELTA_SYNC_INTERVAL_SECONDS`
   - Firestore batch com `FIRESTORE_BATCH_SIZE`
   - Offline cache TTL com `LOCAL_CACHE_TTL_MINUTES`

---

## Apêndice B: Firebase Upload Script Update

Adicionar ao `.specs/sdd-051-remote-config-evolution-maintenance/firebase-upload.sh`:

```bash
# Add to CONFIG_JSON parameters section:

"api_request_timeout_seconds": {
  "description": "API request timeout (seconds)",
  "valueType": "NUMBER",
  "defaultValue": { "value": "30" }
},
"feat_ai_vision_fallback_enabled": {
  "description": "Enable Gemini Vision fallback (disable if broken)",
  "valueType": "BOOLEAN",
  "defaultValue": { "value": "true" }
},
"feat_auto_add_sample_products": {
  "description": "Auto-add sample products on list creation",
  "valueType": "BOOLEAN",
  "defaultValue": { "value": "false" }
},
"wear_full_sync_interval_minutes": {
  "description": "Wear OS full sync interval (minutes)",
  "valueType": "NUMBER",
  "defaultValue": { "value": "15" }
}
# ... (18 more)
```

---

## Apêndice C: Next Steps Timeline

```
2026-11-08: sdd-052 (plano aprovado)
2026-11-15: sdd-052 implementação (adicionar 20 configs a RemoteConfigKeys/Defaults)
2026-11-22: sdd-053 (retry logic API/Network)
2026-11-29: sdd-054 (UX/Onboarding)
2026-12-06: sdd-055 (Wear OS & Sync)
```

Total: 4 semanas para 3 SDDs + implementação completa de todas as 20 configs.
