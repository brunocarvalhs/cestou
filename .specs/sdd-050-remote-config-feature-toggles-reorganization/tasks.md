# Task: Reorganizar Feature Flags por Funcionalidade

- **Slug:** remote-config-feature-toggles-reorganization
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-08
- **Status:** ready
- **Versão-alvo:** 2.3.0
- **Tipo de mudança:** refactor
- **Impacto público (módulo):** none (reorganização interna, sem breaking changes)

## 1. Contexto e Motivação

RemoteConfigKeys.kt expõe 20+ toggles e variáveis organizadas cronologicamente (por SDD). Isso dificulta:
- **Descoberta:** um dev novo não sabe onde procurar um toggle de "AppFunctions" se não conhecer sdd-046/047/048/049
- **Manutenção:** toggles relacionados (ex: `PROACTIVE_AI`, `APP_FUNCTIONS_*`) ficam espalhados
- **Rastreabilidade:** comentários ligam toggles a SDDs, não a funcionalidades

Demanda: reorganizar por **funcionalidade semântica**, não por SDD, mantendo:
- Zero breaking changes (nomes de constantes imutáveis)
- Comentários que explicam **por que** cada toggle existe
- Relacionamentos entre toggles documentados
- Mapeamento de funcionalidades aos toggles

Rastreabilidade:
- Arquivo atual: `core/remote-config/src/main/java/br/com/brunocarvalhs/howmuch/core/remoteconfig/model/RemoteConfigKeys.kt:1-80`
- Consumidores: qualquer módulo via `FeatureFlagService.isEnabled()` e `RemoteVariableService.getString()`

## 2. Escopo

- **Inclui:**
  - Reorganização de constantes em 5 categorias semânticas (`FeatureFlagKeys`)
  - Reorganização de variáveis em 3 categorias semânticas (`RemoteVariableKeys`)
  - Adição de comentários explicativos (não apenas referência a SDD)
  - Documentação de interdependências (ex: `PROACTIVE_AI` requer `APP_FUNCTIONS_ENABLED`)
  - Mapeamento visual (tabela: funcionalidade → toggles)

- **Exclui:**
  - Mudança de nomes de constantes (zero breaking changes)
  - Novo comportamento em features
  - Alterações em FeatureFlagService ou RemoteVariableService
  - Remoção de toggles

## 3. Impacto Arquitetural

**Módulo:** `core:remote-config` (modelo de dados)

**Camadas afetadas:**
- Data (RemoteConfigKeys.kt): organização de constantes
- Nenhuma mudança em domain ou presentation

**Novo arquivo:** Nenhum. Apenas reorganização dentro de RemoteConfigKeys.kt.

**Diagrama de fluxo (sem mudança):**

```
┌────────────────────────────────────────────────┐
│ FeatureFlagService.isEnabled(key)              │
└────────────────────────────────────────────────┘
                      ↓
         ┌─────────────────────────────┐
         │ FeatureFlagKeys.* (const)   │ ← REORGANIZADO
         │ (1) AI & LLM                │
         │ (2) Auth & Identity         │
         │ (3) Shopping & Automation   │
         │ (4) Feedback & Analytics    │
         │ (5) Platform Integration    │
         └─────────────────────────────┘
                      ↓
         ┌─────────────────────────────┐
         │ Firebase Remote Config      │
         │ (value default: on/off)     │
         └─────────────────────────────┘
```

**Nenhuma mudança em DI, injeção ou visibilidade.**

## 4. Contratos e Interfaces

### Mapeamento de Funcionalidades → Toggles (novo)

```
CATEGORIA 1: AI Assistant & LLM Integration
├── feat_ai_openrouter_enabled           (provedor OpenRouter)
├── feat_ai_gemini_enabled               (provedor Gemini)
├── feat_proactive_ai                    (sugestões no chat)
├── [VAR] ai_openrouter_api_key          (credencial)
├── [VAR] ai_openrouter_model            (modelo)
├── [VAR] ai_gemini_api_key              (credencial)
├── [VAR] ai_gemini_agent_model          (modelo)
└── [VAR] ai_system_prompt               (comportamento)

CATEGORIA 2: Authentication & Identity
├── feat_auth_google_enabled             (login Google)
├── feat_auth_email_enabled              (login E-mail)
├── feat_auth_phone_enabled              (login Phone)
├── feat_auth_apple_enabled              (login Apple)
├── feat_auth_microsoft_enabled          (login Microsoft)
├── feat_auth_facebook_enabled           (login Facebook)
└── feat_auth_github_enabled             (login GitHub)

CATEGORIA 3: Shopping List Management & Automation
├── feat_app_functions                   (base: criar, adicionar, resumo)
├── feat_app_functions_extended          (estendida: marcar, remover, orçamento)
├── feat_app_functions_tier1             (busca, lote, listagem, detalhes)
├── feat_app_functions_tier2             (duplicação, receitas, analytics)
├── feat_app_functions_tier3             (preço, reabertura, membros, compartilhar)
└── feat_budget_alerts_enabled           (avisos de orçamento)

CATEGORIA 4: User Feedback & Analytics
├── feat_feedback_enabled                (pesquisas NPS/CSAT + thumb)
├── feat_analytics_ui_actions            (rastreamento de cliques)
├── feedback_store_review_enabled        (pedido In-App Review)
├── [VAR] feedback_nps_cooldown_days     (mínimo entre surveys)
└── [VAR] feedback_csat_cooldown_days    (mínimo entre survey de recurso)

CATEGORIA 5: Platform Integration
└── feat_wear_login_enabled              (pareamento relógio)
```

**Nenhuma constante será renomeada** — apenas reorganizadas no arquivo com comentários regionais.

**Exemplo de novo layout:**

```kotlin
object FeatureFlagKeys {
    // ===== CATEGORY 1: AI Assistant & LLM Integration =====
    // Habilita OpenRouter como provedor de LLM. Requer credencial.
    const val AI_OPENROUTER_ENABLED = "ai_openrouter_enabled"
    
    // Habilita Gemini Vision API como provedor LLM + OCR. Requer credencial.
    const val AI_GEMINI_ENABLED = "ai_gemini_enabled"
    
    // Habilita sugestões proativas no chat (chips, ações).
    // Dependência: requer AI_OPENROUTER_ENABLED ou AI_GEMINI_ENABLED.
    const val PROACTIVE_AI_ENABLED = "feat_proactive_ai"
    
    // ===== CATEGORY 2: Authentication & Identity =====
    // Google Sign-In. Default on (Play reviewer must always sign in).
    const val AUTH_GOOGLE_ENABLED = "auth_google_enabled"
    
    // ... etc
}
```

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem |
|---|---|---|---|
| 1 | Toggles espalhados por SDD (cronologia) | Toggles agrupados por funcionalidade (semântica) | Developer experience |
| 2 | Comentários ligam toggle a SDD | Comentários explicam propósito + dependências | `RemoteConfigKeys.kt:1-80` |
| 3 | Nomes de constantes imutáveis | Nomes permanecem idênticos (zero breaking changes) | Consumer compatibility |
| 4 | Sem documentação visual | Mapeamento funcionalidade → toggle adicionado | `sdd-050/tasks.md` |

## 6. Critérios de Aceitação

**Reorganização:**
- ✓ 5 categorias semânticas bem definidas (AI, Auth, Shopping, Feedback, Platform)
- ✓ Todos os 20 toggles/variáveis mapeados em categoria
- ✓ Nenhuma constante renomeada (zero breaking changes)
- ✓ Comentários explicam **por que**, não apenas **o quê**
- ✓ Interdependências documentadas (ex: "requer X para funcionar")

**Documentação:**
- ✓ Mapeamento tabular (funcionalidade → toggles) em `sdd-050/tasks.md`
- ✓ Exemplo de novo layout em `sdd-050/tasks.md`
- ✓ Guia de adição de novos toggles no futuro

**Qualidade de código:**
- ✓ Lint/Detekt: sem findings
- ✓ Nenhum teste quebrado (zero breaking changes)

## 7. Plano de Testes

**Unitários:**
- Verificar que todas as constantes continuam acessíveis (ex: `FeatureFlagKeys.APP_FUNCTIONS_ENABLED`)
- Verificar que `FeatureFlagService.isEnabled()` continua funcionando
- Verificar que `RemoteVariableService.getString()` continua funcionando

**Lint/Detekt:**
- `./gradlew lintDebug` — sem findings
- `./gradlew detekt` — sem findings

**Compatibilidade:**
- Nenhum arquivo de código de produção deve quebrar (constantes continuam com mesmo nome)
- Verificar que builds anteriores que referenciavam constantes ainda compilam

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Reorganização quebra consumidores | Muito baixa (nomes imutáveis) | Alto | Nenhuma (garantia: zero breaking changes) |
| Comentários ficam desatualizados | Média | Média | Revisar ao adicionar novo toggle |
| Mapeamento visual sai de sincronia | Média | Baixa | Manutenção via `sdd-050/tasks.md` |

## 9. Perguntas em Aberto (bloqueantes)

- [ ] Nenhuma.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (`RemoteConfigKeys.kt:1-80`).
- [x] Nenhum código de produção escrito (apenas pseudocódigo/organização).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Mudança em símbolo público reflete `none` (zero breaking changes).
- [x] Nenhum novo toggle — apenas reorganização.
- [x] Mapeamento visual criado.
- [x] Perguntas em aberto exauridas.

---

## Apêndice: Template para Adições Futuras

Quando adicionar novo toggle, siga:

```kotlin
// CATEGORIA: <Funcionalidade>
// 
// **Nome:** <breve descrição>
// **Propósito:** <por que existe>
// **Dependências:** <outros toggles necessários, se houver>
// **Default:** on/off
// **SDD:** <referência se aplicável>
const val NEW_FEATURE_ENABLED = "feat_new_feature"
```

Exemplo:
```kotlin
// CATEGORIA: Shopping List Management & Automation
//
// **Nome:** Histórico de Compras
// **Propósito:** Armazena e exibe histórico de listas finalizadas
// **Dependências:** nenhuma
// **Default:** on
// **SDD:** sdd-051
const val SHOPPING_HISTORY_ENABLED = "feat_shopping_history"
```
