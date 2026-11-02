# Task: Proteção contra crashes por dados inválidos ou sessão expirada

- **Slug:** crash-hardening
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** fix
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

Várias operações lançam exceções não tratadas ao encontrar dados inválidos ou sessão expirada: parse inseguro de horário na UI (C-02), `getOrCreateUserId()` sem sessão chamado fora de `runCatching` em telas de carrinho/produtos (C-03), `valueOf` ao reidratar o histórico do chat (C-04), `value!!`/`whereIn` sem validação no cliente Firestore (C-05, latente) e `LocalViewModelStoreOwner.current!!` (C-06, latente).

Revalidado no código em 2026-10-27 (este plano substitui a versão anterior do mesmo arquivo — ver nota no rodapé). `ShoppingReminderScheduler` já resolve o parse seguro internamente (`parseTime`, linhas 55-59); o item C-02 é sobre reaproveitar essa lógica na tela, que ainda usa `toInt()` direto.

**Itens do backlog (`.specs/sdd-001-auditoria-geral/tasks.md:35-39`):** C-02, C-03, C-04, C-05, C-06.

## 2. Escopo

**Inclui:**
- **C-02** — Extrair o parse seguro de horário já existente em `feature/settings/.../data/worker/ShoppingReminderScheduler.kt:55-59` (`parseTime`, `coerceIn` + fallback 18:00) para `core:common` e reutilizá-lo em `feature/settings/.../presentation/screen/NotificationSettingsScreen.kt:95-98` (hoje `timeParts[0].toInt()` / `timeParts[1].toInt()` sem proteção).
- **C-03** — Blindar os chamadores diretos (não-AI-agent) de `AuthService.getOrCreateUserId()` que hoje não estão dentro de um `runCatching`:
  - `feature/products/.../domain/usecase/ProductSaveUseCase.kt:47-53` (overload `invoke(product, shoppingId)`, usado por `ProductFormViewModel.kt:105` e `ProductPhotoViewModel.kt:123,164`).
  - `feature/cart/.../presentation/viewmodel/EditItemViewModel.kt:48`.
  - `feature/cart/.../presentation/viewmodel/ConfirmItemViewModel.kt:48`.
  - `feature/products/.../data/repository/CommonProductRepositoryImpl.kt:28-30` (`getAll()`, builder `flow {}` sem `catch`; hoje só é consumido dentro de um `runCatching` em `CommonProductAddAllToShoppingUseCase.kt:27-29`, mas o `Flow` em si não é seguro para futuros coletores — corrigir na fonte).
  - **Não inclui:** `ProductSaveUseCase.execute(...)` (linha 77) — já protegido pelo chokepoint `try/catch` em `GeminiAgent.executeFunctionCall` (`GeminiAgent.kt:133-156`) e `OpenRouterAgent.executeToolCall` (`OpenRouterAgent.kt:178-195`); e `ShoppingCreateUseCase.kt:43`, `ShoppingDuplicateUseCase.kt:25`, `ShoppingJoinUseCase.kt:20`, `ShoppingRepositoryImpl.kt:47,95` — já envolvidos em `runCatching`/`.getOrNull()`, nenhuma ação necessária.
- **C-04** — `ChatMessageMapper.kt:9` (`ChatMessage.Sender.valueOf(sender)`) e seu único chamador `ChatHistoryRepositoryImpl.kt:17-21` (`load`): descartar mensagens com `sender` inválido em vez de propagar a exceção.
- **C-05** — `FirestoreClient.kt:143,146,149` (`applyFilter`): validar `value` antes de `whereArrayContains`/`whereIn` em vez de `value!!` e de depender da `IllegalArgumentException` nativa do SDK do Firestore.
- **C-06** — `ProductScreen.kt:53` (`LocalViewModelStoreOwner.current!!`): substituir por `checkNotNull` com mensagem explícita.

**Exclui:**
- Novas dependências externas.
- Mudança de assinatura pública de `AuthService.getOrCreateUserId()` (mantém `suspend fun getOrCreateUserId(): AuthenticatedUser`, lançando em caso de ausência de sessão) — contrato usado em 9+ pontos de 3 módulos (`feature:shopping`, `feature:products`, `feature:cart`); a correção é blindar os chamadores, não o contrato (ver §4).
- Tratamento global de `CoroutineExceptionHandler` (fica para refactor futuro, se necessário).
- `ProductSaveUseCase.execute(...)`, `ShoppingCreateUseCase`, `ShoppingDuplicateUseCase`, `ShoppingJoinUseCase`, `ShoppingRepositoryImpl` — já seguros, confirmado no código (ver C-03 acima).

## 3. Impacto Arquitetural

```
:core:common/util
  └── ReminderTimeParser.kt  [novo, public]
      └── parse(reminderTime: String): Pair<Int, Int>   // hour to minute, fallback 18:00

:feature:settings
  ├── data/worker/ShoppingReminderScheduler.kt   [mudança: delega para ReminderTimeParser, remove parseTime privado]
  └── presentation/screen/NotificationSettingsScreen.kt   [mudança: usa ReminderTimeParser.parse em vez de toInt() direto]

:feature:products
  ├── domain/usecase/ProductSaveUseCase.kt   [mudança: invoke(product, shoppingId) envolve corpo em runCatching]
  └── data/repository/CommonProductRepositoryImpl.kt   [mudança: getAll() captura falha de getOrCreateUserId, emite lista vazia]

:feature:cart/presentation/viewmodel
  ├── EditItemViewModel.kt   [mudança: onSaveEdit trata falha de getOrCreateUserId como o onFailure já existente]
  └── ConfirmItemViewModel.kt   [mudança: onConfirmPurchased, mesmo padrão]

:feature:chat/data
  ├── mapper/ChatMessageMapper.kt   [mudança: toDomain() passa a ser nullable/runCatching]
  └── repository/ChatHistoryRepositoryImpl.kt   [mudança: load() usa mapNotNull]

:core:data/network
  └── FirestoreClient.kt   [mudança: applyFilter valida value antes de whereArrayContains/whereIn]

:feature:products/presentation/screen
  └── ProductScreen.kt   [mudança: checkNotNull com mensagem em vez de !!]
```

Nenhum módulo novo. Nenhuma mudança de camada (todas as alterações ficam na camada já responsável: `data`/`presentation`/`domain` de cada feature, e um novo utilitário puro em `core:common`).

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Notas |
|---|---|---|---|
| `ReminderTimeParser.parse(reminderTime: String): Pair<Int, Int>` | Novo | `public` (`core:common`) — precisa ser `public` porque é consumido por `feature:settings`, módulo diferente de `core:common` | Lógica idêntica à já existente e testada implicitamente em `ShoppingReminderScheduler`; apenas promovida para função reutilizável |
| `ShoppingReminderScheduler.parseTime` | Removido (privado) | — | Substituído pela chamada a `ReminderTimeParser.parse` |
| `ChatMessageModel.toDomain(): ChatMessage?` | Mudança de retorno (`ChatMessage` → `ChatMessage?`) | `internal` (mapper já é `internal fun` dentro do módulo `feature:chat`) | Sem impacto público — único chamador é `ChatHistoryRepositoryImpl` no mesmo módulo |
| `ProductSaveUseCase.invoke(product, shoppingId): Result<Unit>` | Assinatura inalterada; corpo passa a capturar exceção | `public` (classe `class ProductSaveUseCase`, já pública) | Comportamento: hoje pode lançar apesar do tipo `Result`; depois do fix, sempre retorna `Result` |
| `AuthService.getOrCreateUserId(): AuthenticatedUser` | **Inalterado** | `public` (`core:domain`) | Decisão explícita de não quebrar o contrato (ver §2 Exclui) |
| `CommonProductRepository.getAll(): Flow<List<CommonProduct>>` | Assinatura inalterada; `Flow` passa a nunca lançar | `public` (interface de módulo) | Falha de sessão agora emite lista vazia, como o `.catch` já faz para falhas de rede na mesma função |

Nenhum `NavKey`, modelo de domínio público, enum ou contrato de IA/remote-config é alterado. Nenhuma coleção Firestore, endpoint ou evento de Analytics é criado.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Horário de lembrete inválido/legado quebra o seletor de horário na UI | Parse seguro (reaproveitado de `ShoppingReminderScheduler`) com fallback 18:00 | `NotificationSettingsScreen.kt:95-98` |
| 2 | Sessão expirada ao salvar/editar/confirmar um produto propaga exceção através de um `Result<Unit>` que prometia nunca lançar | Falha de sessão vira `Result.failure` (use case) ou estado de erro na UI (ViewModels), sem crash | `ProductSaveUseCase.kt:47-53`, `EditItemViewModel.kt:48`, `ConfirmItemViewModel.kt:48` |
| 3 | `CommonProductRepository.getAll()` pode lançar dentro do `Flow` se não houver sessão | Flow emite lista vazia nesse caso, mesmo padrão do `.catch` já usado para erro de rede | `CommonProductRepositoryImpl.kt:28-30` |
| 4 | Histórico de chat persistido com `sender` desconhecido (versão antiga/corrupção) quebra `load()` inteiro | Mensagem inválida é descartada individualmente; demais mensagens carregam normalmente | `ChatMessageMapper.kt:9`, `ChatHistoryRepositoryImpl.kt:17-21` |
| 5 | Filtro Firestore com valor nulo em campo array, ou lista vazia/>30 itens em `whereIn`, depende de exceção nativa do SDK | Validação explícita na entrada com mensagem clara (hoje latente — nenhum chamador real ainda) | `FirestoreClient.kt:143,146,149` |
| 6 | Composição fora de um `ViewModelStoreOwner` (preview/teste) causa NPE sem contexto | `checkNotNull` com mensagem explícita (hoje latente) | `ProductScreen.kt:53` |

## 6. Critérios de Aceitação

- **`ReminderTimeParser.parse`:** `parse("18:30")` → `18 to 30`; `parse("")`, `parse("abc")`, `parse("99:99")` → fallback `18 to 0` (mesmo comportamento hoje coberto implicitamente por `ShoppingReminderScheduler`, agora explícito e testável isoladamente).
- **`NotificationSettingsScreen`:** abrir o seletor de horário com `state.reminderTime` vazio ou corrompido não lança `NumberFormatException`/`IndexOutOfBoundsException`; o seletor abre em 18:00.
- **`ProductSaveUseCase.invoke(product, shoppingId)`:** sem sessão → `Result.failure`, nunca lança. `ProductFormViewModel`/`ProductPhotoViewModel` continuam tratando via `.onFailure`/`onSuccess` já existentes, sem mudança nesses ViewModels.
- **`EditItemViewModel.onSaveEdit` / `ConfirmItemViewModel.onConfirmPurchased`:** sem sessão → `uiState.error` preenchido com `UiText`, `isLoading = false`, nenhuma exceção não tratada no `viewModelScope`.
- **`CommonProductRepositoryImpl.getAll()`:** sem sessão → `Flow` emite `emptyList()` em vez de lançar.
- **`ChatHistoryRepositoryImpl.load`:** histórico com uma mensagem de `sender` inválido → as demais mensagens válidas são retornadas; nenhuma exceção sobe para o chamador.
- **`FirestoreClient.applyFilter`:** `value == null` em campo de `ARRAY_FIELDS` ou `_contains` → `IllegalArgumentException` com mensagem clara, lançada antes de chamar o SDK. `value is List<*>` vazia ou com mais de 30 itens → mesma validação explícita (hoje dependia do SDK).
- **`ProductScreen`:** `LocalViewModelStoreOwner.current` nulo → `IllegalStateException` com mensagem explicando o contexto esperado, não NPE genérica.
- **Dados legados:** `ReminderTimeParser` e `ChatMessageMapper` continuam aceitando os formatos válidos atuais sem mudança de comportamento observável para o usuário.
- Nenhum feature toggle é necessário — todas as mudanças são correções de robustez sem alteração de comportamento visível no caminho feliz.

## 7. Plano de Testes

**Unitários (novos ou a estender):**
- `core/common/src/test/.../ReminderTimeParserTest.kt` [novo]: `parse("18:30")`, `parse("")`, `parse("abc:12")`, `parse("25:70")`.
- `feature/settings/src/test/.../NotificationSettingsScreenTest.kt` ou teste de estado equivalente: `reminderTime` inválido não quebra a composição (se já existir suíte Compose para a tela; caso não exista, cobrir via teste do `ReminderTimeParser` é suficiente, já que a tela passa a delegar 100% do parse).
- `feature/products/src/test/.../ProductSaveUseCaseTest.kt` [estender]: `invoke(product, shoppingId)` com `authService.getOrCreateUserId()` lançando `IllegalStateException` → `Result.failure`, não propaga.
- `feature/products/src/test/.../CommonProductRepositoryImplTest.kt` [estender]: `getAll()` com `getOrCreateUserId()` lançando → `Flow` emite `emptyList()`.
- `feature/cart/src/test/.../EditItemViewModelTest.kt` [estender]: `authService.getOrCreateUserId()` lançando → `uiState.error != null`, `isLoading == false`.
- `feature/cart/src/test/.../ConfirmItemViewModelTest.kt` [estender]: mesmo cenário.
- `feature/chat/src/test/.../ChatMessageMapperTest.kt` [novo]: `ChatMessageModel(sender = "INVALID").toDomain()` → `null`; `ChatMessageModel(sender = "USER").toDomain()` → `ChatMessage` válido.
- `feature/chat/src/test/.../ChatHistoryRepositoryImplTest.kt` [estender]: histórico com uma mensagem de `sender` inválido entre duas válidas → `load()` retorna só as duas válidas.
- `core/data/src/test/.../FirestoreClientTest.kt` [estender]: filtro em `ARRAY_FIELDS`/`_contains` com `value = null` → `IllegalArgumentException` com mensagem; `whereIn` com lista vazia ou 31 itens → `IllegalArgumentException`.
- `feature/products` — teste instrumentado existente (se houver) ou novo teste de Compose para `ProductScreen` sem `LocalViewModelStoreOwner`: espera `IllegalStateException` com mensagem (não NPE). Se não houver harness simples para isso, documentar como verificação manual no PR.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| `ReminderTimeParser` divergir do `parseTime` original e mudar o fallback observado pelo usuário | baixa | baixo | Copiar a lógica exata de `ShoppingReminderScheduler.kt:55-59` (mesmas constantes `DEFAULT_HOUR=18`, `MAX_HOUR=23`, `MAX_MINUTE=59`), não reescrever |
| Envolver `ProductSaveUseCase.invoke` em `runCatching` mudar comportamento de outros `Result.failure` já esperados (ex.: validação de `save()`) | baixa | médio | `save(...)` já retorna `Result`; `runCatching` externo só precisa capturar a resolução de `userId`, preservando o `Result` interno — revisar com teste existente de `ProductSaveUseCaseTest` |
| Descartar mensagens de chat inválidas perder contexto do usuário | baixa | baixo | Log de aviso (`Timber`) ao descartar; não afeta UX, pois a mensagem já estava corrompida |
| `CommonProductRepositoryImpl.getAll()` emitir lista vazia mascarar um problema real de sessão sem sinalização | baixa | baixo | Mesmo padrão já aceito para erro de rede na mesma função (`.catch` + `Timber.e`); manter log explícito |

## 9. Perguntas em Aberto

Nenhuma bloqueante.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (revalidado no código em 2026-10-27, não apenas herdado do sdd-001).
- [x] Nenhum código de produção escrito (apenas assinaturas e descrição de mudança).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Nenhuma mudança em contrato público que exija `breaking`/bump além do patch já planejado (2.2.2).
- [x] Nenhum endpoint/remote-config novo.
- [x] Nenhum cache novo.
- [x] Perguntas em aberto exauridas.

---
**Nota de regeneração:** este plano substitui a versão anterior do mesmo arquivo (mesma data, mesmo autor). A revisão no código confirmou todos os 5 itens do backlog como ainda presentes, mas restringiu C-03 ao conjunto exato de chamadores hoje desprotegidos — `ProductSaveUseCase.execute(...)` e os use cases de `feature:shopping` já estão protegidos por `try/catch`/`runCatching` existentes e foram explicitamente excluídos do escopo (§2) para evitar trabalho redundante.

**Nota de implementação (2026-10-27, Code-Implementer):** todos os itens do escopo (§2) foram implementados — ver relatório na conversa para a lista de arquivos alterados/criados. `lintDebug`, `detekt`, `testDebug`, `koverVerify` e `koverVerifyLayout` passaram no repositório inteiro. Nenhum commit foi criado (fora do escopo desta skill). Pendente: revisão com `code-reviewer` antes de commit/PR.
