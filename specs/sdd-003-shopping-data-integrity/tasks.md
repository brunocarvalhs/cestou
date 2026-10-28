# Task: Integridade de dados em operações de compra e lista

- **Slug:** shopping-data-integrity
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-27 (revisão 2 — plano anterior reescrito após verificação no código pós-`sdd-002`)
- **Status:** ready
- **Versão-alvo:** 2.2.2
- **Tipo de mudança:** fix
- **Impacto público (módulo):** breaking — `ShoppingRepository` ganha `updateFields` (aditivo) e muda o comportamento de erro de `getAll()` (deixa de engolir falhas); `Shopping` ganha o campo `establishment: String?` (aditivo).

> **Nota de revisão:** este arquivo já existia com `status: ready`, datado antes da implementação do `sdd-002-network-error-contract` (commit `245db26e`). Reescrevi o plano do zero após reler todo o código citado, porque o contrato de erro mudou (`NetworkService.make()` agora lança `NetworkException` em vez de retornar `null`) e isso: (a) resolveu uma parte do bug B-02 (criar em vez de atualizar a lista duplicada) e (b) **expôs um bug novo e mais grave no mesmo fluxo** (ver §1, duplicar produtos). Os `arquivo:linha` abaixo refletem o código atual, não o plano antigo.

## 1. Contexto e Motivação

Várias escritas em `feature:shopping`, `feature:cart` e `feature:products` reaproveitam o documento inteiro (`Shopping` completo) a partir de snapshots potencialmente desatualizados, ou usam o método errado (`update` em vez de `save`) para documentos que ainda não existem. Isso perde edições concorrentes (B-04), impede remover o orçamento (B-05), quebra a duplicação de listas (B-02) e pode apagar um produto ao movê-lo (B-17). A exclusão de conta pode deixar listas órfãs se a leitura falhar (B-25).

**Itens do backlog:** B-02, B-03, B-04, B-05, B-17, B-25 (`specs/sdd-001-auditoria-geral/tasks.md` §2.2).

**O que mudou desde que `sdd-002` foi implementado** (verificado lendo o código atual, não o plano antigo):

| Item | Era (plano antigo) | É agora (código lido) | Fonte |
|---|---|---|---|
| B-02(b) — lista duplicada usa `update` em doc novo | Bug na própria lista | **Corrigido**: `repository.create(duplicatedShopping)` já é usado. | `ShoppingDuplicateUseCase.kt:26` |
| B-02(a) — `collect` sobre listener nunca termina | Bug | **Ainda presente**, confirmado: `getAllProducts` é um `networkService.observe` (listener Firestore, nunca completa sozinho) e o use case faz `.collect {}` sobre ele. | `ShoppingDuplicateUseCase.kt:29`; `ProductRepositoryImpl.kt:102-114` |
| **B-02(b′) — NOVO, mesmo bug migrou para os produtos** | não documentado assim | Dentro do `collect`, cada produto copiado é salvo com `productsUseCase.update(...)` (Firestore `PUT`→`.update()`) para um **id novo que nunca foi criado** na lista destino. Antes do sdd-002 isso falhava em silêncio (B-01); agora `networkService.make()` lança `NetworkException` (NOT_FOUND), e isso aconteceria a cada duplicação — só não é visível hoje porque (a) trava a coroutine antes de chegar aqui. | `ShoppingDuplicateUseCase.kt:31-34`; `ProductRepositoryImpl.updateProduct:131-145` (usa PUT); `saveProduct:86-100` (usa POST, correto para doc novo) |
| B-02(c) — copia `users`/`roles`/`shortCode` | Bug | **Ainda presente**, e o próprio repositório já documenta isso como bug conhecido não corrigido. | `ShoppingDuplicateUseCase.kt:18-24`; `firestore.rules:108-112`; `architecture/05-decisoes-arquiteturais.md:121,172-175` (AD-009) |
| B-03 — finalizar sobrescreve `description` | Bug | **Ainda presente**, e agora com um risco novo: `repository.update()` propaga `NetworkException` (sdd-002) e a chamada não está em `runCatching`/`try-catch` — uma falha de rede nesse ponto agora **derruba a coroutine do ViewModel** em vez de silenciosamente "funcionar". | `FinishPurchaseViewModel.kt:41-59` |
| B-04 — escrita de documento inteiro perde edição concorrente | Bug | **Ainda presente.** `FirestoreClient.put()` já usa `.update()` no Firestore (merge no nível do SDK), mas como o chamador sempre monta o mapa a partir do objeto `Shopping` inteiro (`shopping.toModel().toMap()`), toda escrita sobrescreve 100% dos campos — o merge do SDK não ajuda. `leave()` e `updatePositions()` já mostram o padrão correto (mapa parcial). | `ShoppingRepositoryImpl.kt:136-145` (`update`), `178-191` (`leave`, correto), `226-244` (`updatePositions`, correto) |
| B-05 — não dá para remover o orçamento | Bug | **Ainda presente**: `FirestoreClient.put()` filtra `null` do mapa antes de enviar (`data.filterKeys{...}.filterValues{it != null}`), então um `budget = null` simplesmente não é enviado. `FieldOp.Delete` já existe e funciona (usado em `leave`), só não é usado para `budget`. | `FirestoreClient.kt:99` |
| B-17 — mover produto apaga antes de salvar | Bug | **Ainda presente**, ordem confirmada: `delete(...).getOrThrow(); save(...).getOrThrow()`. | `ProductsUseCaseImpl.kt:24-29` |
| B-25 — excluir conta pode deixar listas órfãs | Bug | **Ainda presente, mas a causa raiz é mais local do que o plano antigo supunha.** `DeleteAccountUseCase` já está correto (`runCatching` em volta de tudo, falha se algo lançar). O problema é que `ShoppingRepositoryImpl.getAll()` tem seu **próprio** `try/catch` que converte qualquer `Exception` (inclusive a `NetworkException` do sdd-002) em lista vazia — o sdd-002 nunca chega a propagar. `ShoppingReminderWorker` já tem um `try/catch` em volta de `getAll()` esperando que ele lance (código morto hoje). | `ShoppingRepositoryImpl.kt:91-107`; `DeleteAccountUseCase.kt:17-37`; `ShoppingReminderWorker.kt:40-47` |

## 2. Escopo

**Inclui:**
- `ShoppingRepository.updateFields(id: String, fields: Map<String, Any?>): Result<Unit>` — novo contrato para escritas parciais (mesmo padrão de `updatePositions`/`leave`).
- `Shopping.establishment: String?` — novo campo (ver §4, P-03 resolvido).
- Duplicar lista: `users=[eu]`, `roles={eu: OWNER}`, `shortCode=null`, `price=0.0`, resto copiado; produtos lidos com `.first()` (não `.collect`) e salvos com `save()` (não `update()`), em paralelo.
- Finalizar compra: `updateFields(id, {status, price, establishment})` — preserva `description`; só navega/notifica em caso de sucesso; mostra erro em caso de falha.
- Editar lista (`EditShoppingViewModel`): `updateFields` só com os campos editáveis na tela (`title`, `description`, `isCategorized`, `budget`, `emoji`), nunca `users`/`roles`/`status`/`position`/`shortCode`/`createdAt`.
- Reabrir lista (`ShoppingReopenUseCase`): migrar de `update(shopping.copy(status=...))` para `updateFields(id, {status})` — mesma classe de bug (B-04), correção trivial, mesmo módulo.
- Mover produto: salvar no destino primeiro, depois apagar na origem.
- `ShoppingRepositoryImpl.getAll()`: remover o `try/catch` que converte erro em lista vazia, deixando `NetworkException` propagar (alinhado ao contrato do sdd-002). Ajustar os dois chamadores que dependiam do comportamento antigo (ver §4).
- Deletar conta: nenhuma mudança de código nesse arquivo — o fix de `getAll()` acima já resolve B-25 (`runCatching` do use case passa a capturar a falha de verdade).

**Exclui:**
- Reordenação manual por `position` (B-06, fica para `sdd-008-shopping-rules`).
- `getById()` ter o mesmo padrão de swallow-to-null de `getAll()` — mesma classe de problema, mas nenhum item deste SDD depende disso; mexer nela afetaria `ShoppingUpdateUseCase`, `CartViewModel`, etc., fora do escopo aprovado aqui. Registrado como risco observado (§8), não corrigido.
- `checkIsLocked` fazer uma leitura por produto ao salvar em lote (B-20, fica para `sdd-008` ou feature própria).
- Histórico de auditoria (feature futura).
- Sincronização offline (2.3.0).
- Publicar `firestore.rules` (D-01, decisão separada — o comentário em `firestore.rules:108-112` acompanha o fix de B-02(c) deste SDD, mas o deploy da regra não é escopo daqui).

## 3. Impacto Arquitetural

```
:core:domain
  └── repository/ShoppingRepository.kt
      └── updateFields(id: String, fields: Map<String, Any?>): Result<Unit>   [novo, public]
      └── getAll(): List<Shopping>  [assinatura igual; comportamento de erro muda — propaga em vez de engolir]
  └── model/Shopping.kt
      └── establishment: String? = null   [novo campo, public]

:feature:shopping
  ├── data/repository/ShoppingRepositoryImpl.kt
  │     ├── updateFields(...)       [novo método — mesmo padrão de updatePositions/leave]
  │     └── getAll()                [remove o try/catch local; deixa propagar]
  ├── data/model/ShoppingModel.kt + data/mapper/ShoppingMapper.kt
  │     └── establishment            [novo campo no model + mapper toDomain/toModel/toMap]
  ├── domain/usecase/ShoppingDuplicateUseCase.kt
  │     └── users=[eu], roles={eu:OWNER}, shortCode=null, price=0.0
  │         productsUseCase(id).first() (não collect) + save() em paralelo (não update())
  ├── domain/usecase/ShoppingUpdateUseCase.kt
  │     └── invoke(id, fields: Map<String, Any?>) via repository.updateFields  [assinatura muda]
  ├── domain/usecase/ShoppingReopenUseCase.kt
  │     └── updateFields(id, {"status" to IN_PROGRESS})
  └── presentation/viewmodel/EditShoppingViewModel.kt
        └── monta Map só com os campos editáveis, chama shoppingUpdateUseCase(id, fields)
        └── presentation/wear/viewmodel/LastListViewModel.kt
              └── runCatching em volta de shoppingRepository.getAll() (preserva fallback "sem lista")

:feature:cart
  └── presentation/viewmodel/FinishPurchaseViewModel.kt
        └── updateFields(id, {status, price, establishment}) com Result;
            navega/notifica só em onSuccess; novo estado de erro na UI

:feature:products
  └── domain/usecase/ProductsUseCaseImpl.kt
        └── move(): save(destino) antes de delete(origem)

:feature:settings
  └── domain/usecase/DeleteAccountUseCase.kt   [nenhuma mudança de código — corrigido via getAll()]
  └── data/worker/ShoppingReminderWorker.kt    [nenhuma mudança — já tem o try/catch certo, hoje morto]
```

Injeção via Hilt inalterada (nenhum módulo novo). `ShoppingDuplicateUseCase` passa a injetar `AuthService` (já disponível via `core:domain`, usado do mesmo jeito que `ShoppingRepositoryImpl` o usa).

## 4. Contratos e Interfaces

| Contrato | Mudança | Visibilidade | Justificativa / Notas |
|---|---|---|---|
| `ShoppingRepository.updateFields(id: String, fields: Map<String, Any?>): Result<Unit>` | **Novo, aditivo** | `public` (interface em `core:domain`, implementação em `feature:shopping`) | Precisa ser `public`: é chamado por `feature:shopping` (impl.), `feature:cart` (`FinishPurchaseViewModel`) e indiretamente por `feature:ai-agent` via `ShoppingUpdateUseCase`, todos módulos diferentes do que a declara. Mesmo padrão já aprovado em `updatePositions`/`leave`. |
| `Shopping.establishment: String? = null` | **Novo, aditivo** | `public` (`core:domain`, persistido no Firestore) | P-03 resolvido (ver análise abaixo): a tela de finalizar compra já tem um campo de UI dedicado (`shopping_list_label_establishment`, `FinishPurchaseContent.kt:88-91`), prova de que "estabelecimento" é um dado distinto de `description` na intenção do produto, não um sinônimo. Valor padrão `null` mantém documentos legados legíveis sem migração. |
| `ShoppingRepository.getAll(): List<Shopping>` | Assinatura igual; **comportamento muda** (propaga falha em vez de devolver lista vazia) | `public` | Não é breaking de assinatura, mas é breaking de contrato comportamental (AGENTS.md §6 exige registrar). Ver análise de impacto abaixo. |
| `ShoppingUpdateUseCase.invoke(id: String, fields: Map<String, Any?>): Result<Unit>` | Assinatura muda (era `invoke(id, updatedShopping: Shopping)`) | `internal` (chamado só por `EditShoppingViewModel` no mesmo módulo e pelo próprio `execute()` do agente de IA) | Hoje já constrói um mapa de campos informados antes de montar o `Shopping` completo (`ShoppingUpdateUseCase.kt:44-58`) — só falta parar de "desmontar" esse mapa de volta em objeto completo antes do `update()`. |

**Análise de impacto de `getAll()` propagar erro** (chamadores confirmados via grep, excluindo testes):

| Chamador | Hoje | Depois de remover o swallow | Mudança necessária |
|---|---|---|---|
| `DeleteAccountUseCase.kt:26` | `runCatching { getAll().forEach {...} ... }` | A falha agora chega ao `runCatching` → `Result.failure` de verdade | **Nenhuma** (já correto) |
| `ShoppingReminderWorker.kt:45-48` | `try { getAll()... } catch (e: Exception) { ...; return Result.retry() }` | O `catch` finalmente é alcançável | **Nenhuma** (já correto, hoje é código morto) |
| `ShoppingGetAllUseCase.kt:17-19` | `runCatching { repository.getAll() }` | Propaga para `Result.failure`, que é o contrato esperado por uma `AgentActionUseCase` | **Nenhuma** (já correto) |
| `LastListViewModel.kt:17-22` | `shoppingRepository.getAll().filter{...}.maxByOrNull{...}?.id` sem try/catch | Uma falha de rede derrubaria `lastListId()`, chamado sem try/catch em `ShoppingWearGraph.kt:30` | **Envolver em `runCatching { }.getOrDefault(emptyList())`**, preservando o fallback gracioso atual (Wear é tráfego degradado, ver AD-009) |

Nenhuma nova coleção, evento de Analytics ou remote-config neste plano.

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento após a mudança | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Duplicar lê produtos com `collect` sobre um listener que nunca termina. | Lê com `.first()` (um snapshot) e retorna. | `ShoppingDuplicateUseCase.kt:29` |
| 2 | Duplicar salva cada produto com `update()` num id que não existe → `NOT_FOUND`. | Salva com `save()` (POST). | `ShoppingDuplicateUseCase.kt:31-34`; `ProductRepositoryImpl.kt:86-100` |
| 3 | Duplicar copia `users`, `roles` e `shortCode` do original. | Cópia privada: `users=[eu]`, `roles={eu:OWNER}`, `shortCode=null`. | `ShoppingDuplicateUseCase.kt:18-24` |
| 4 | Finalizar sobrescreve `description` com o nome do estabelecimento e grava o documento inteiro. | Atualização parcial (`status`, `price`, `establishment`); `description` preservada; navega só em sucesso. | `FinishPurchaseViewModel.kt:41-48` |
| 5 | Edições (editar lista, reabrir) gravam o `Shopping` inteiro a partir de um snapshot possivelmente desatualizado. | `updateFields` com só os campos que a operação de fato muda. | `EditShoppingViewModel.kt` (via `ShoppingUpdateUseCase`); `ShoppingReopenUseCase.kt:29` |
| 6 | Orçamento não pode ser removido (`null` é descartado antes do Firestore). | `budget = null` vira `NetworkService.FieldOp.Delete` em `updateFields`. | `FirestoreClient.kt:99,103-104` (mecanismo já existe) |
| 7 | Mover produto apaga na origem antes de confirmar o salvamento no destino. | Salva no destino primeiro; só apaga na origem se o salvamento teve sucesso. | `ProductsUseCaseImpl.kt:24-29` |
| 8 | `getAll()` engole falha de rede e devolve lista vazia. | Propaga a falha (`NetworkException`); `DeleteAccountUseCase` falha com `Result.failure` em vez de prosseguir sem saber se há listas. | `ShoppingRepositoryImpl.kt:91-107` |

## 6. Critérios de Aceitação

- **`updateFields`:** só altera as chaves passadas no mapa. Chaves omitidas não mudam no Firestore. Valor `null` em um campo anulável (`budget`, `establishment`, `shortCode`) deve ser convertido para `NetworkService.FieldOp.Delete` pelo chamador antes de passar o mapa (o `FirestoreClient` já trata `FieldOp.Delete`; não adicionar essa conversão dentro do `FirestoreClient`, que é agnóstico de modelo).
- **Duplicar:** termina em tempo finito (sem usar `collect` sobre Flow infinito — teste com `runTest` e timeout). Produtos copiados aparecem na lista nova. `users == [currentUserId]`, `roles == {currentUserId: "OWNER"}`, `shortCode == null`. `price == 0.0`. Falha ao salvar um produto não interrompe silenciosamente os demais (usar `awaitAll` e deixar a primeira falha propagar via `runCatching` externo, igual ao padrão de `updatePositions`).
- **Finalizar:** `description` original preservada. `establishment` gravado em campo próprio. Em falha de rede, a tela mostra erro (novo estado, não há `UiState` hoje — ver §3) e **não** navega nem notifica os membros. Notificação só acontece após confirmação de sucesso do `updateFields`.
- **Editar lista / reabrir:** o mapa de campos enviado contém só os campos que aquela tela/operação pode mudar; nunca inclui `users`, `roles`, `position`, `createdAt`, `shortCode`.
- **Mover produto:** `save(destino)` deve suceder antes de `delete(origem)` ser chamado. Se `save` falhar (ex.: lista destino finalizada via `checkIsLocked`), o produto continua intacto na origem.
- **Deletar conta:** se `shoppingRepository.getAll()` lançar, `DeleteAccountUseCase` retorna `Result.failure` e não chama `userRepository.deleteProfile`/`authService.deleteAccount`. `LastListViewModel.lastListId()` continua retornando `null` graciosamente nesse mesmo cenário (não deve propagar para o grafo de navegação do Wear).
- **Dados legados:** documentos do Firestore sem `establishment` continuam decodificando (`String? = null`). `updateFields` não depende de nenhum campo novo existir no documento atual.
- **Performance:** nenhuma escrita em lote fica serial onde pode ser paralela (seguir o padrão `async`/`awaitAll` de `updatePositions.kt:226-244` para os produtos duplicados). Nenhuma leitura redundante nova introduzida.
- **Observabilidade:** nenhum evento novo necessário neste SDD; os eventos existentes (`CART_FINISH_PURCHASE_COMPLETED`, `SHOPPING_BUDGET_SET`) continuam disparando apenas em sucesso.
- **Toggles:** nenhum necessário — são correções de bug em fluxos já ativos, sem necessidade de rollout controlado.

## 7. Plano de Testes

**Unitários (JUnit + coroutines-test, fakes de `ShoppingRepository`/`NetworkService`):**
- B-02: `ShoppingDuplicateUseCase` termina em `runTest` sem timeout; chama `repository.create` uma vez e `productsUseCase.save` N vezes (nunca `update`); resultado tem `users == [uid]`, `roles == {uid: "OWNER"}`, `shortCode == null`, `price == 0.0`.
- B-02 (regressão): falha ao salvar um produto propaga como `Result.failure` do use case (não trava, não ignora silenciosamente).
- B-03: `onFinishPurchase` chama `updateFields(id, mapOf("status" to FINISH, "price" to price, "establishment" to establishment))` e **não** inclui `description`. Em sucesso, chama `notifyOtherMembers` e `navigator.goBack()`. Em falha (`updateFields` retorna `Result.failure`), nenhum dos dois é chamado e o estado de erro é setado.
- B-04: `EditShoppingViewModel.update` chama `shoppingUpdateUseCase` com um mapa contendo só `title`/`description`/`isCategorized`/`budget`/`emoji` alterados — nunca `users`/`roles`/`status`/`position`.
- B-04 (reopen): `ShoppingReopenUseCase` chama `updateFields(id, mapOf("status" to IN_PROGRESS))`.
- B-05: `budget = null` em qualquer fluxo de `updateFields` chega ao `FirestoreClient` como `NetworkService.FieldOp.Delete` (teste de integração do mapper/use case com fake de `NetworkService` verificando o payload).
- B-17: `ProductsUseCaseImpl.move` — fake de `save` que falha (lista destino finalizada) ⇒ `delete` nunca é chamado; produto permanece na origem.
- B-25: fake de `ShoppingRepositoryImpl`/`NetworkService` que lança em `getAll()` ⇒ `DeleteAccountUseCase` retorna `Result.failure` e `deleteProfile`/`deleteAccount` nunca são chamados (reforça os testes já existentes em `DeleteAccountUseCaseTest.kt`, que hoje usam `coEvery { getAll() } returns ...` — adicionar um caso `throws`).
- `ShoppingRepositoryImplTest`: `getAll()` propaga a exceção do `NetworkService` em vez de devolver `emptyList()` (os 3 testes existentes em `ShoppingRepositoryImplTest.kt:99,109,119` precisam de revisão — hoje provavelmente esperam `emptyList()` no caminho de erro).
- `LastListViewModelTest`: `getAll()` lança ⇒ `lastListId()` retorna `null` (novo caso, usando o mesmo fake já presente no arquivo).

**UI (Robolectric/JVM, seletores por `testTag`, sem emulador — `feature:cart`/`feature:products` não têm `androidTest` hoje, só `app`/`core:ui`/`feature:settings`):**
- B-03: `FinishPurchaseContent` — digitar no campo de estabelecimento e confirmar reporta só esse valor em `onFinish`; a tela não tem acesso a `Shopping.description`, então não pode mais confundir os dois. Implementado em `CartComponentsTest.kt`.
- B-17 (parcial): a parte "produto continua na origem" já está coberta no nível correto (`ProductsUseCaseImplTest`, unitário). A parte "mensagem de erro aparece" **não foi implementada**: `CartViewModel.moveProduct` não expõe nenhum estado de erro para `CartScreen` hoje (nenhum fluxo de escrita do carrinho expõe — é exatamente a lacuna do item F-01, fora de escopo deste SDD). Adicionar essa UI de erro exigiria tocar `CartUiState`/`CartViewModel`/`CartScreen` fora do que este plano aprovou; fica registrado como pré-requisito do próximo SDD que tratar F-01.

**Lint/Detekt/Cobertura:**
```bash
./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout
```

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| `getAll()` propagar erro muda comportamento para todos os chamadores ao mesmo tempo. | baixa (só 4 chamadores, 3 já preparados) | médio se `LastListViewModel` não for ajustado | Ajustar `LastListViewModel` neste mesmo PR (§4); grep de `.getAll()` antes de fechar a revisão. |
| `getById()` tem o mesmo padrão de swallow que `getAll()`, não corrigido aqui. | média | baixo neste SDD (nenhum item depende disso) | Registrar como item para `sdd-008` ou um SDD próprio de hardening de leitura; não expandir escopo aqui. |
| Publicar `firestore.rules` (D-01, fora de escopo) pode voltar a permitir a `allow create` de `shortCodes` falhar de outra forma quando duplicar grava `shortCode=null` — index simplesmente não é criado, o que é o comportamento correto (lista privada nova ainda não tem código de convite até o usuário compartilhar). | baixa | baixo | Nenhuma ação — comportamento desejado, `ensureShortCodeIndex` já cobre o backfill quando o usuário pedir para compartilhar. |
| `ShoppingUpdateUseCase` é usado pelo agente de IA (`execute()`); mudar a assinatura de `invoke` pode quebrar esse caminho se não for revisado com atenção. | média | médio | Cobrir com teste o caminho `execute()` → `invoke(id, fields)`, não só o caminho da UI. |
| Novo campo `establishment` em `Shopping`/`ShoppingModel` exige atualizar `ShoppingMapperTest` e qualquer outro teste que construa `Shopping`/`ShoppingModel` com todos os campos nomeados. | alta (é um `data class` com testes exaustivos) | baixo | Grep de `Shopping(` e `ShoppingModel(` com todos os parâmetros nomeados antes de compilar; a maioria quebra em tempo de compilação, não em runtime. |

## 9. Perguntas em Aberto (não bloqueantes — resolvidas com evidência de código, sinalizadas para confirmação do maintainer)

- [x] **P-02** (B-02): a cópia de uma lista compartilhada deve ser privada (`users=[eu]`)? **Resolvido: sim.** É a única leitura consistente com o comentário já presente em `firestore.rules:108-112` e `architecture/05:172-175`, que descrevem o comportamento atual como bug a corrigir exatamente nesse sentido. Orçamento é preservado (mesma finalidade de compra); `price` (gasto acumulado) é zerado.
- [x] **P-03** (B-03): criar `Shopping.establishment` ou descartar? **Resolvido: criar.** A tela já tem um campo de formulário dedicado (`FinishPurchaseContent.kt:88-91`, strings `shopping_list_label_establishment`/`shopping_list_placeholder_establishment`), evidência de que o produto já trata "estabelecimento" como informação própria — hoje só é perdida porque o código joga esse valor dentro de `description`.

Nenhuma pergunta bloqueante restante.

## 10. Checklist de Conformidade

- [x] Decisões citam `arquivo:linha` (todas as tabelas acima) e `architecture/05` (AD-009) onde existe.
- [x] Nenhum código de produção escrito (só pseudocódigo/assinaturas/diagramas).
- [x] Seção 6 preenchida de forma substantiva, incluindo performance e observabilidade.
- [x] Mudança em símbolo público reflete `breaking`/`additive` em "Impacto público" e na tabela de §4 (`updateFields`, `Shopping.establishment`, comportamento de `getAll()`).
- [x] Novo endpoint/remote-config: nenhum. Novo cache: nenhum.
- [x] Perguntas em aberto exauridas (P-02 e P-03 resolvidas com evidência; nenhuma bloqueante).
