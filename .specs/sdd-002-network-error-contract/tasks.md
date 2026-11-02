# Task: Contrato de erro de rede e feedback de falha

- **Slug:** network-error-contract
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** breaking
- **Impacto público (módulo):** breaking

## 1. Contexto e Motivação

O `NetworkService.make()` em `core/data/.../network/NetworkServiceImpl.kt:71-77` engole toda exceção e retorna `null`. Os repositórios envolvem essa chamada em `runCatching` esperando falhas que nunca chegam. Por isso, gravações que falham parecem ter dado certo, perdendo dados silenciosamente (B-01). Este SDD estabelece o novo contrato de erro e garante que falhas de rede sejam propagadas e exibidas ao usuário (F-01).

**Itens do backlog:** B-01, F-01; D-01 (só verificação da data).

## 2. Escopo

**Inclui:**
- Mudar `NetworkService.make()` para lançar `NetworkException` em caso de falha (ou retornar `Result<T?>`, a decidir em revisão).
- Migrar todos os repositórios (`core:data` e `feature:*/data`) para tratar `NetworkException` ou `Result.failure`.
- Adicionar tipo `UiText` ou similar em `core:common` para mensagens de erro traduzidas (pt-BR e en-US).
- Adicionar feedback visual (snackbar/Toast) em VMs onde há escrita: `EditItemViewModel`, `ConfirmItemViewModel`, `CartViewModel`, `ShoppingListViewModel`, etc.
- Verificar a data de expiração da regra placeholder no Firestore console (`how-much-2a72e`) — se estiver vencida ou próxima, estender temporariamente.

**Exclui:**
- Publicação das `firestore.rules` (fica para o fim da Onda 0, depende de D-01 ser endereçado).
- Implementação de retry automático ou exponential backoff (fica para `sdd-009-firestore-decoding`).
- Push notifications (fica para F-04 em 2.3.0).

## 3. Impacto Arquitetural

```
:core:domain
  └── services/NetworkService.kt
      └── lança NetworkException(cause, httpCode?) ou retorna Result<T?>
      
:core:data
  └── network/NetworkServiceImpl.kt
      └── propaga exceção em vez de engolir
      
:core:common
  └── UiText (novo, multilingue)
  
:core:domain
  └── repository/* (interfaces)
      └── retornam Result<T> (já existem alguns)
      
:feature:*/data/repository/*Impl
  ├── ShoppingRepositoryImpl.kt
  ├── ProductRepositoryImpl.kt
  ├── UserRepositoryImpl.kt
  ├── NotificationRepositoryImpl.kt
  ├── RecipeRepositoryImpl.kt
  └── [outros]
      └── migram para tratar NetworkException ou Result.failure
      
:feature:*/presentation/viewmodel/*ViewModel
  ├── EditItemViewModel.kt
  ├── ConfirmItemViewModel.kt
  ├── CartViewModel.kt
  ├── ShoppingListViewModel.kt
  └── [outros]
      └── expõem UiText de erro no estado de UI
      └── exibem snackbar/toast ao falhar
```

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Notas |
|---|---|---|---|
| `NetworkService.make<T>(...)` | **BREAKING**: lança `NetworkException` em falha | `public` | alternativa: retorna `Result<T?>` |
| `NetworkException(cause, httpCode?)` | Novo | `public` (`core:domain`) | mapeado em `Result.failure` nos repos |
| `UiText` | Novo | `public` (`core:common`) | suporta `@StringRes` e strings dinâmicas |
| `ShoppingRepository.save/update/delete` | Já retornam `Result<Unit>` (verificar) | `public` | todos os repos alinhados |

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após | Origem |
|---|---|---|---|
| 1 | Gravação falha aparece como sucesso | Falha é propagada, exibida e armazenada no estado | `NetworkServiceImpl.kt:71-77` |
| 2 | Sem feedback de erro para o usuário | Snackbar/Toast com mensagem traduzida em erro de escrita | F-01 |

## 6. Critérios de Aceitação

- **Tipo de exceção:** `NetworkException` com `cause` (rede, HTTP 403/404/500, JSON parse, etc.) e `httpCode` opcional.
- **Propagação:** nenhum `runCatching` silencia `NetworkException`. Se houver, é tratado no ViewModel.
- **UI:** VMs que fazem escrita (`save`, `delete`, `update`) expõem `uiError: UiText?` no estado. Composables exibem snackbar ou erro inline.
- **Strings:** toda mensagem de erro em `strings.xml` (pt-BR e en-US). Exemplo: `error_network`, `error_permission_denied`, `error_not_found`.
- **D-01 verificação:** Confirmar data de expiração da regra placeholder. Se <7 dias, estender para data futura e registrar em `CHANGELOG.md`.

## 7. Plano de Testes

**Unitários:**
- `NetworkServiceImpl`: `make` lança `NetworkException` em cada cenário (rede indisponível, HTTP 5xx, JSON inválido, permissão negada, etc.).
- Cada repositório: fake de `NetworkService` que lança exceção → `Result.failure` propagado.
- `EditItemViewModel`: falha de escrita → `uiError` contém mensagem traduzida.
- `CartViewModel.finishPurchase`: falha → `uiError` preenchido, tela não navega.

**Instrumentados:**
- Desconectar rede no emulador → tentar adicionar produto → snackbar com erro.
- (Opcional) Mock de HTTP 403 via interceptor → verificar mensagem específica.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Placeholder do Firestore expirado, derrubando o app | desconhecida (verificar hoje) | total | Estender data ou publicar regras (onda 1) |
| Expor erros novos causa confusion no usuário | média | médio | Mensagens claras, testes de UX |
| Mudar contrato quebra chamadores esquecidos | média | médio | Grep `networkService.make` + testes |

## 9. Perguntas em Aberto

- [ ] **P-01a:** Data de expiração da regra placeholder no console (must-do hoje)?
- [ ] **P-01b:** `Result<T?>` ou exceção lançada? (recomendação: exceção, mais simples)

Se P-01a indicar data <7 dias, marcar este SDD como bloqueado até resolver D-01.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` e `sdd-001-auditoria-geral`.
- [x] Nenhum código escrito (só assinaturas).
- [x] Seção 6 substantiva.
- [x] Breaking change em `NetworkService.make` registrado.
- [x] Strings em pt-BR e en-US definidas.
- [x] Testes unitários e instrumentados planejados.
- [x] Risco D-01 mitigado (verificação hoje).
