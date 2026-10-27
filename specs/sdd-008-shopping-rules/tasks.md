# Task: Regras de negócio do shopping (reordenação, status, join, papéis)

- **Slug:** shopping-rules
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** feature
- **Impacto público (módulo):** additive

## 1. Contexto e Motivação

A reordenação manual das listas não se mantém: ignora `position` e usa `createdAt`/nome (B-06). O status `IN_PROGRESS` nunca é definido automaticamente (B-18). Entrar em lista é feito antes de validação; reentrar notifica todos (B-14). Status `FINISHED` pode aceitar entrada (indefinido, P-08). Papéis inconsistentes em listas duplicadas (B-02, já tratado em sdd-003). Este SDD implementa ordenação manual, transições de status corretas e validações de join.

**Itens do backlog:** B-06, B-14, B-18.

## 2. Escopo

**Inclui:**
- Modo de ordenação `MANUAL` por `position` em `ShoppingListViewModel`.
- Cálculo de `position` sobre a lista completa, não filtrada.
- Transição `NEW` → `IN_PROGRESS` ao marcar o primeiro item como comprado.
- Validar join: se já é membro, não notificar (idempotência).
- Decidir se lista `FINISHED` aceita novos membros (P-08, proposta: não).
- Atualizar `architecture/02` com nova regra de status.

**Exclui:**
- UI de reordenação (toque + arrastar, fica para feature separada).
- Sincronização de `position` offline (fica para 2.3.0).
- Push notification de join (fica para F-04).

## 3. Impacto Arquitetural

```
:core:domain
  └── model/Shopping.kt
      ├── sortMode: SortMode = MANUAL  [novo enum]
      └── position: Int  [já existe?]
      
:feature:shopping/presentation/viewmodel
  └── ShoppingListViewModel.kt
      ├── ordenarPorPosition (novo metodo)
      └── aplicar ao renderizar listas
      
:feature:shopping/domain/usecase
  ├── ShoppingReopenUseCase.kt
  │   └── NEW → IN_PROGRESS ao marcar comprado
  └── ShoppingJoinUseCase.kt
      ├── validar se já membro antes de notificar
      └── rejeitar entrada se FINISHED (P-08)
      
:architecture/02-regras-de-negocio.md
  └── atualizar §1 com novo status behavior
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Notas |
|---|---|---|---|
| `SortMode` | Novo enum | `public` (`core:domain`) | MANUAL, BY_NAME, BY_DATE |
| `Shopping.sortMode` | Novo campo | `public` | padrão MANUAL |
| `ShoppingJoinUseCase` | Validação nova | `public` (use case) | idempotência, P-08 |

## 5. Regras de Negócio Aplicáveis

| # | Regra | Comportamento novo | Origem |
|---|---|---|---|
| 1 | Ordenação usa `createdAt` ou nome | Modo `MANUAL` por `position`; cálculo sobre lista completa | B-06 |
| 2 | `NEW` só muda ao reabrir | `NEW` → `IN_PROGRESS` ao primeiro item comprado | B-18 |
| 3 | Join é feito antes de validar | Validar antes; se já membro, não notificar (idempotente) | B-14 |
| 4 | Lista `FINISHED` aceita entrada | Rejeitar (P-08, proposta: não) | B-14 |

## 6. Critérios de Aceitação

- **Ordenação manual:** lista exibe por `position` (crescente). Arrasto (UI) futuro atualiza `position` via `ShoppingRepository.updatePositions`.
- **Cálculo de posições:** quando filtro ativo, renderizar filtra, mas `position` sempre é calculado sobre todas as listas.
- **Status NEW → IN_PROGRESS:** ao marcar primeiro item como comprado (em `CartViewModel.markAsPurchased`), atualizar `status` se `status == NEW`.
- **Join validação:** verificar email/id antes de gravar. Se já membro, responder com sucesso, sem notificar.
- **FINISHED rejeita:** se P-08 = não, `ShoppingJoinUseCase` retorna erro tipado ao tentar entrar em lista finalizada.
- **Compatibilidade:** listas antigas sem `sortMode` herdam `MANUAL` (ou usam algoritmo antigo transitoriamente).
- **Documentação:** `architecture/02` §1 cita novo behavior de status.

## 7. Plano de Testes

**Unitários:**
- B-06: Listar 5 listas com `position=[2,4,1,3,5]` → renderiza em ordem `[1,2,3,4,5]`.
- B-06: Filtrar (nome contém "A") → renderiza 3, mas `position` calculada sobre 5.
- B-18: Marcar item como comprado em lista `status=NEW` → status vira `IN_PROGRESS`.
- B-14: Join com `shoppingId` já membro → sem notificação. Resultado: sucesso, `role` mantido.
- B-14: Join em lista `status=FINISHED` (P-08=não) → erro retornado.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Adicionar `sortMode` quebra documentos legados | baixa | médio | Migração/fallback a MANUAL |
| Mudança de status confunde UX | baixa | baixo | Teste manual de transições |
| P-08 indefinido bloqueia decisão | média | médio | Decidir hoje, marcar como `blocked` se não |

## 9. Perguntas em Aberto

- [ ] **P-08:** Lista `FINISHED` aceita novos membros? (recomendação: não)

Se P-08 não for respondido, marcar B-14 como BLOCKED.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (B-06, B-14, B-18).
- [x] Nenhum código escrito.
- [x] Seção 6 substantiva.
- [x] `SortMode` é aditivo.
- [x] Testes unitários planejados.
- [ ] P-08 respondida.
