# Task: Integridade de dados em operações de compra e lista

- **Slug:** shopping-data-integrity
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** fix
- **Impacto público (módulo):** breaking (adiciona `ShoppingRepository.updateFields`)

## 1. Contexto e Motivação

Operações de escrita reutilizam documentos completos e desatualizados (snapshots antigos de navegação), ou usam `update()` em vez de `save()` em documentos novos (retornando `NOT_FOUND`). Isso resulta em perda de edições de outros membros (B-02, B-03, B-04) e em remoção impossível de orçamento (B-05). Além disso, duplicar lista copia membros e papéis quando deveria privá-la (B-02). Este SDD corrige essas operações com atualizações parciais e fluxos transacionais.

**Itens do backlog:** B-02, B-03, B-04, B-05, B-17, B-25.

## 2. Escopo

**Inclui:**
- `ShoppingRepository.updateFields(id, Map<String, Any?>)` — novo contrato para escritas parciais.
- Usar `FieldOp.Delete` para remover campos anuláveis (orçamento, estabelecimento, etc.).
- Duplicar lista: `users=[eu]`, `roles={eu: OWNER}`, `shortCode` novo, copiar produtos (não metadados).
- Finalizar compra: atualizar só `status`, `price` e possível campo `establishment` (conforme P-03).
- Mover produto: salvar no destino, depois apagar na origem (ou `WriteBatch`).
- Deletar conta: falhar se a leitura de listas falhar (depende de sdd-002).

**Exclui:**
- Reordenação manual (B-06, fica para sdd-008).
- Histórico de auditoria (fica para feature futura).
- Sincronização offline (fica para 2.3.0).

## 3. Impacto Arquitetural

```
:core:domain
  └── repository/ShoppingRepository.kt
      └── updateFields(id: String, fields: Map<String, Any?>): Result<Unit>  [novo]
      
:feature:shopping/data/repository/ShoppingRepositoryImpl.kt
  ├── ShoppingDuplicateUseCase.kt
  │   └── duplicar: users=[currentUser], roles={..}, shortCode novo, save produtos
  ├── ShoppingUpdateUseCase.kt
  │   └── updateFields em vez de documento completo
  ├── FinishPurchaseViewModel.kt
  │   └── atualizar parcial (status, price, establishment?)
  └── updateFields impl. (novo método)
      └── FirestoreClient.updateFields(...)
      
:feature:cart/presentation/viewmodel/CartViewModel.kt
  └── mover/deletar com WriteBatch ou transação
  
:feature:settings/domain/usecase/DeleteAccountUseCase.kt
  └── falhar se getAll() falhar
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Notas |
|---|---|---|---|
| `ShoppingRepository.updateFields(id, Map)` | Aditivo | `public` | usa `FirestoreClient.updateFields` |
| `FirestoreClient.updateFields(...)` | Novo | `internal` | aplica `FieldOp.Delete` para campos nulos |
| `Shopping` (modelo) | Possível novo campo `establishment` | `public` | B-03, P-03 a decidir |

## 5. Regras de Negócio Aplicáveis

| # | Regra | Comportamento novo | Origem |
|---|---|---|---|
| 1 | Duplicar copia membros e papéis | Cópia privada: `users=[eu]`, `roles={eu: OWNER}` | B-02 |
| 2 | Finalizar sobrescreve `description` | Preserva `description`, atualiza `status`/`price` parcialmente | B-03 |
| 3 | Edições simultâneas perdem atualizações | Usar `updateFields` (parcial) em vez de documento completo | B-04 |
| 4 | Orçamento não pode ser removido | Pode ser removido com `FieldOp.Delete` | B-05 |
| 5 | Mover produto pode perdê-lo | Salvar destino, depois apagar origem | B-17 |
| 6 | Deletar conta pode deixar listas órfãs | Falhar se leitura falhar (depende sdd-002) | B-25 |

## 6. Critérios de Aceitação

- **`updateFields`:** só altera campos passados. Campos omitidos não mudam. Nulos enviam `FieldOp.Delete`.
- **Duplicar:** `shortCode` novo (com retry de colisão se B-31 implementado). Cópia tem 0 histórico ou cópia do original?
- **Finalizar:** descrição preservada. Estabelecimento em campo novo ou descartado (P-03)?
- **Mover:** duas escritas ou `WriteBatch`. Nenhum caminho pode deletar sem salvar.
- **Deletar conta:** se `ShoppingRepository.getAll()` falhar, `DeleteAccountUseCase` falha com `Result.failure`.
- **Dados legados:** `updateFields` não quebra documentos antigos.

## 7. Plano de Testes

**Unitários:**
- B-02: Duplicar termina sem timeout, copia produtos, `users=[eu]`, `shortCode` diferente.
- B-03: `updateFields(id, {status, price})` preserva `description`. Navegar só se sucesso.
- B-04: Dois repos simultâneos, um faz `updateFields({name})`, outro `updateFields({budget})` → ambos aplicados.
- B-05: `updateFields(id, {budget: null})` envia `FieldOp.Delete`.
- B-17: Mover com destino finalizado → erro ao salvar → origem não deletada.
- B-25: `getAll()` falha → `DeleteAccountUseCase` retorna `Result.failure`.

**Instrumentados:**
- B-03: Editar lista (descrição) e finalizar simultaneamente → descrição preservada.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| `updateFields` quebra código legado | baixa | médio | Testes + grep de `set(Shopping)` |
| Duplicar sem remover lista antigos | média | baixo | Documentação clara |
| P-03 (campo `establishment`) indeciso | média | médio | Decidir antes de revisar, usar rascunho se incerto |

## 9. Perguntas em Aberto

- [ ] **P-03:** Criar `Shopping.establishment` ou descartar? (afeta B-03)

Se indeciso, marcar B-03 como "atualizar só `status` e `price`".

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` e backlog (B-02 a B-05, B-17, B-25).
- [x] Nenhum código escrito.
- [x] Seção 6 substantiva.
- [x] `ShoppingRepository.updateFields` é aditivo.
- [x] Testes planejados para cada item.
