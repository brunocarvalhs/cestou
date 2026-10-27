# Task: Confiabilidade do Google Play Billing

- **Slug:** billing-hardening
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** fix
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

Billing: `refresh` ignora `responseCode` (B-24); `connect()` pode ficar suspenso para sempre; `refresh` só roda no paywall (KDoc diz "on every app resume"); entitlement é por aparelho, não por conta. Play indisponível → lista vazia → um PRO vira FREE. Assinatura cancelada continua PRO até o app reiniciar. Este SDD corriga conexão com timeout/retry, refresh automático no resume e validação de resposta.

**Itens do backlog:** B-24.

## 2. Escopo

**Inclui:**
- `connect()` com timeout (ex.: 10 segundos) e retry.
- `refresh()` chamado em `ProcessLifecycleOwner.ON_RESUME` (não só no paywall).
- Validar `responseCode == OK` antes de atualizar entitlement.
- Entitlement por conta (Firebase Auth uid), não por aparelho (fica para P-07).
- Log de falhas em `core:analytics` sem dados pessoais.

**Exclui:**
- Implementação do backend (P-07, fica para 2.3.0+).
- Migração de aparelho (fica para feature futura).

## 3. Impacto Arquitetural

```
:core:billing
  └── repository/PlayBillingSubscriptionRepository.kt
      ├── connect() com timeout + retry  [mudança]
      ├── refresh() em ON_RESUME  [mudança]
      ├── validar responseCode == OK  [mudança]
      └── log de erros (analytics)  [novo]
      
:app
  └── MainViewModel.kt
      ├── ProcessLifecycleOwner.ON_RESUME listener  [novo]
      └── call billingRepository.refresh()
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Notas |
|---|---|---|---|
| `PlayBillingSubscriptionRepository.connect` | Timeout + retry | `public` | dependência interna |
| `PlayBillingSubscriptionRepository.refresh` | Público + ON_RESUME | `public` | chamador externo |

## 5. Regras de Negócio Aplicáveis

| # | Regra | Comportamento novo | Origem |
|---|---|---|---|
| 1 | Play indisponível → lista vazia | Só atualizar se `responseCode == OK`; manter status antigo caso contrário | B-24 |
| 2 | `connect()` sem timeout | Timeout 10s + retry com backoff | B-24 |
| 3 | `refresh` só no paywall | Automático em ON_RESUME | B-24 |
| 4 | Assinatura cancelada continua PRO | Status atualizado em cada resume | B-24 |

## 6. Critérios de Aceitação

- **`connect()` timeout:** espera máximo 10 segundos; se falhar, retry com backoff (2s, 4s, máximo de 3 tentativas).
- **`refresh()` automático:** `ProcessLifecycleOwner` listener em `MainViewModel` chama `billingRepository.refresh()` em ON_RESUME.
- **Validação de resposta:** `if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) updateEntitlement()` senão, manter estado.
- **Entitlement por conta:** hoje é por aparelho (cache local). Com P-07 (backend), verificar uid do Firebase.
- **Log de erros:** `billingClient.connection failed (timeout)` em analytics (sem PII).
- **Compatibilidade:** não quebra chamadores existentes de `connect()` (ex.: `PaywallViewModel`).

## 7. Plano de Testes

**Unitários:**
- B-24: `refresh()` retorna `BILLING_UNAVAILABLE` → status não muda. `OK` → atualiza.
- B-24: `connect()` falha → retry com backoff; depois sucesso.
- B-24: ON_RESUME dispara → `refresh()` chamado automaticamente.

**Instrumentados:**
- Desligar Play Services no emulador → app mostra PRO da sessão anterior. Religar + ON_RESUME → status atualizado se cancelado.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Timeout 10s é insuficiente | baixa | médio | Ajustar se testes mostrarem | B-24 |
| ON_RESUME refresh causa latência | baixa | baixo | Cache local, não bloqueia UI |
| P-07 (backend) muda contrato | média | alto | Planejado para depois, isolar lógica |

## 9. Perguntas em Aberto

- [ ] **P-07:** Há plano de sair do Spark para Blaze (Cloud Functions) para verificação de compra no servidor? (afeta entitlement por conta)

Se P-07 = não, marcar "entitlement por aparelho" como design final.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (B-24).
- [x] Nenhum código escrito.
- [x] Seção 6 substantiva.
- [x] Não quebra contratos.
- [x] Testes unitários e instrumentados planejados.
