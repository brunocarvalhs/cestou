# Task: Proteção contra crashes por dados inválidos ou sessão expirada

- **Slug:** crash-hardening
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** fix
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

Várias operações lançam exceções não-tratadas ao encontrar dados inválidos ou sessão expirada: parse inseguro de horário (C-02), `getOrCreateUserId()` sem sessão (C-03), `valueOf` ao ler histórico do chat (C-04), e `value!!`/`[0]` em Firestore (C-05, C-06). Este SDD oferece proteção defensiva com parse seguro, `Result` em use cases, e verificações de nulidade.

**Itens do backlog:** C-02, C-03, C-04, C-05, C-06.

## 2. Escopo

**Inclui:**
- Parse seguro de `reminderTime` (extrair função de `ShoppingReminderScheduler.kt:48-53` para `core:common`).
- `getOrCreateUserId()` retorna `Result<String>` ou `String?` em vez de lançar exceção.
- `ChatMessageMapper.valueOf` com `runCatching`, descartar mensagens inválidas.
- `FirestoreClient.whereIn/value!!` com validação de entrada.
- `LocalViewModelStoreOwner.current` com `checkNotNull(message)` ou parâmetro.

**Exclui:**
- Novas dependências.
- Tratamento global de `CoroutineExceptionHandler` (fica para refactor futuro, se necessário).

## 3. Impacto Arquitetural

```
:core:common
  └── TimeParser.kt  [novo]
      └── parseTime(string): Result<Time>
      
:core:auth
  └── FirebaseAuthService.kt
      └── getOrCreateUserId(): Result<String>  [mudança]
      
:feature:chat/data/mapper
  └── ChatMessageMapper.kt
      └── fromDocument: descartar inválidos  [mudança]
      
:core:data/network
  └── FirestoreClient.kt
      └── whereIn/value validam entrada  [mudança]
      
:feature:products/presentation
  └── ProductScreen.kt
      └── LocalViewModelStoreOwner.current com checkNotNull  [mudança]
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Notas |
|---|---|---|---|
| `TimeParser.parseTime(String): Result<LocalTime>` | Novo | `public` (`core:common`) | reutilizável |
| `FirebaseAuthService.getOrCreateUserId(): Result<String>` | Mudança | `public` | ou `String?` |
| `ChatMessageMapper.fromDocument` | Tolera inválidos | `internal` | descarta, não lança |
| `FirestoreClient.whereIn/value` | Validam | `internal` | lançam ou retornam `Result` |

## 5. Regras de Negócio Aplicáveis

| # | Regra | Comportamento novo | Origem |
|---|---|---|---|
| 1 | Horário inválido quebra UI de notificação | Parse seguro retorna padrão (18:00) se inválido | C-02 |
| 2 | Sessão expirada causa crash em operação | Use case retorna `Result.failure`, não lança | C-03 |
| 3 | Histórico corrompido quebra o chat | Mensagens inválidas descartadas, nenhuma exceção | C-04 |
| 4 | Filtro Firestore sem validação lança exceção | Validação na entrada, erro tipado | C-05 |
| 5 | Owner nulo quebra Composable | `checkNotNull` com mensagem ou parâmetro | C-06 |

## 6. Critérios de Aceitação

- **`TimeParser`:** `parseTime("18:30")` → `Success`, `parseTime("")` → `Failure(Invalid)`, fallback a 18:00 na UI.
- **`getOrCreateUserId`:** sem sessão → `Result.failure(NoSessionException)`. Use cases (como `ProductSaveUseCase`) tratam com `onFailure`.
- **`ChatMessageMapper`:** uma mensagem inválida → descartada, não quebra o Flow. Log de aviso.
- **`FirestoreClient.whereIn`:** lista vazia ou >30 itens → `IllegalArgumentException` lançado, capturado na camada de serviço.
- **`LocalViewModelStoreOwner.current`:** `checkNotNull(currentViewModelStoreOwner, "not in owner context")`.
- **Dados legados:** função compatível com formatos antigos (se houver).

## 7. Plano de Testes

**Unitários:**
- C-02: `parseTime("")` → fallback 18:00. `parseTime("99:99")` → fallback.
- C-03: `getOrCreateUserId()` sem sessão → `Result.failure`. `ProductSaveUseCase` trata com `onFailure`.
- C-04: `ChatMessageMapper.fromDocument({sender: "INVALID"})` → descartado, Flow continua.
- C-05: `whereIn([], field)` lança; `whereIn([1..31], field)` lança.
- C-06: Composable sem `LocalViewModelStoreOwner` → `IllegalStateException` com mensagem útil.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Mudança `getOrCreateUserId` quebra chamadores | baixa | médio | Grep + testes de integração |
| Descartar mensagens inválidas perde contexto | baixa | baixo | Log detalhado, não afeta UX |

## 9. Perguntas em Aberto

Nenhuma bloqueante.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (C-02 a C-06).
- [x] Nenhum código escrito.
- [x] Seção 6 substantiva.
- [x] Dados legados suportados.
- [x] Testes unitários planejados.
