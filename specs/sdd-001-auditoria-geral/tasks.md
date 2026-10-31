# Task: Nova Auditoria Geral do Projeto (Crashes, Lógica, Funcionalidades e Débitos)

- **Slug:** auditoria-geral
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-10-31
- **Status:** ready
- **Versão-alvo:** 2.2.2 (correções críticas) e 2.3.0 (novas funcionalidades)
- **Tipo de mudança:** fix
- **Impacto público (módulo):** none (este plano é um documento de backlog arquitetural; novos contratos serão definidos nos SDDs filhos)

> **Nota de atualização (2026-10-31):** Esta auditoria atualiza e substitui o diagnóstico anterior, considerando as melhorias implementadas pelos SDDs 002 a 006 e 011 a 015 (PRs #227 a #236). Os SDDs não executados (007 a 010) foram consolidados neste backlog unificado e limpo.

---

## 1. Contexto e Motivação

Após a entrega das melhorias de estabilidade inicial, arquitetura de configurações, suporte a modelos dinâmicos e experiência do carrinho/busca, realizou-se uma nova varredura estática de ponta a ponta no repositório `develop`.

O objetivo é mapear com precisão cirúrgica (`arquivo:linha`):
1. **Possíveis crashes (C)** ainda latentes;
2. **Bugs de lógica e integridade de dados (B)** remanescentes;
3. **Funcionalidades faltantes (F)** que impactam a experiência do usuário;
4. **Infraestrutura, segurança e conformidade (D)** pendentes (especialmente regras do Firestore e Billing).

Nenhum código de produção é alterado nesta tarefa. Este documento atua como **plano de backlog de referência** para os próximos SDDs.

---

## 2. Escopo do Diagnóstico

### 2.1 Possíveis Crashes e Travamentos (C)

| ID | Sev. | Confirmação | Defeito | Evidência (`arquivo:linha`) | Cenário de Falha | Correção Proposta |
|---|---|---|---|---|---|---|
| **C-01** | P1 | CONFIRMADO | `PlayBillingSubscriptionRepository.connect()` suspende indefinidamente sem timeout. | `core/billing/.../PlayBillingSubscriptionRepository.kt:185` | Processo do Google Play Services morre, trava ou falha na resposta IPC → `suspendCancellableCoroutine` nunca retoma → chamadas `ensureConnected()` travam qualquer fluxo de billing para sempre. | Adicionar `withTimeoutOrNull(10_000L)` e reconexão com retry exponencial. |
| **C-02** | P1 | CONFIRMADO | Spinner infinito no chat se o Flow do assistente concluir sem texto. | `feature/chat/.../AiChatViewModel.kt:70-85` | O modelo responde apenas com tool calls ou resposta em branco → o Flow conclui sem emitir string → `isLoading` nunca é setado para `false` e a UI trava no spinner. | Adicionar `.onCompletion { _uiState.update { it.copy(isLoading = false) } }` no collect do assistente. |
| **C-03** | P2 | CONFIRMADO | `Shopping.Status.valueOf(it)` sem proteção contra casing/incompatibilidade na IA. | `feature/shopping/.../ShoppingUpdateUseCase.kt:57` | O LLM envia `"status": "in_progress"` ou `"finished"` (minúsculo) → `valueOf` lança `IllegalArgumentException` não tratada. | Usar `Shopping.Status.entries.firstOrNull { it.name.equals(statusStr, ignoreCase = true) }`. |
| **C-04** | P2 | CONFIRMADO | `ProductListSaveUseCase` cria produto com nome vazio ao receber vírgula extra. | `feature/products/.../ProductListSaveUseCase.kt:51` | O LLM ou usuário passa `"leite,,pão"` → cria `Product(name = "")` que quebra validações de UI e Firestore. | Aplicar `.filter { it.isNotBlank() }` no split de nomes. |

---

### 2.2 Bugs de Lógica e Integridade de Dados (B)

| ID | Sev. | Confirmação | Defeito | Evidência (`arquivo:linha`) | Cenário de Falha | Correção Proposta |
|---|---|---|---|---|---|---|
| **B-01** | **P0** | CONFIRMADO | Falha temporária da Play Store rebaixa usuário PRO para FREE imediatamente. | `core/billing/.../PlayBillingSubscriptionRepository.kt:101-104` | `queryPurchasesAsync` falha (`responseCode != OK`) → `purchasesList` vem vazio → `handlePurchases(emptyList())` seta `_status.value = FREE` para um assinante legítimo. | Validar `result.responseCode == BillingResponseCode.OK` antes de processar `handlePurchases`. |
| **B-02** | **P0** | CONFIRMADO | `ShoppingUpdateUseCase` permite que o LLM altere ou zere `users` e `roles`. | `feature/shopping/.../ShoppingUpdateUseCase.kt:59-60` | O agente de IA hallucina ou recebe instrução maliciosa e atualiza `roles = {}` → o dono original perde a posse e permissão OWNER da lista. | Remover `users` e `roles` dos argumentos permitidos na tool de atualização da IA. |
| **B-03** | P1 | CONFIRMADO | `moveShopping` calcula posições sobre a lista filtrada, corrompendo a ordenação. | `feature/shopping/.../ShoppingListViewModel.kt:270-282` | Usuário busca por "Mercado" (filtro ativo com 2 itens) e arrasta um item → `mapIndexed` redefine posições como 0 e 1, sobrescrevendo e colidindo com as listas ocultas no Firestore. | Calcular reordenação sobre a lista completa (`uiState.value.list.items`), preservando o índice absoluto. |
| **B-04** | P1 | CONFIRMADO | Ordenação por `position` é ignorada nas atualizações em tempo real. | `feature/shopping/.../ShoppingListViewModel.kt:239-245` | O método `applyFilters()` só ordena por `NAME` ou `createdAt DESC`. Logo após o usuário reordenar, qualquer emissão do snapshot listener desfaz a ordem manual. | Adicionar suporte oficial à ordenação por `position` persistida. |
| **B-05** | P1 | CONFIRMADO | O trial gratuito do chat é consumido antes do envio e até para erros de rede. | `feature/chat/.../AiChatViewModel.kt:56` | `markFreeMessageUsed()` roda antes da resposta. Se a rede falhar ou a API estiver fora, o usuário FREE perde seu único crédito sem receber resposta. | Só invocar `markFreeMessageUsed()` após o primeiro chunk de resposta bem-sucedida, e ignorar se o usuário for PRO. |
| **B-06** | P1 | CONFIRMADO | Botão de envio do chat permite múltiplos cliques concorrentes durante carregamento. | `feature/chat/.../AiChatScreen.kt:220` | `FilledIconButton` valida apenas `isSendEnabled`, mas não `!state.isLoading`. Clicar repetidamente dispara requisições paralelas encavaladas. | Desabilitar botão de envio quando `state.isLoading == true`. |
| **B-07** | P1 | CONFIRMADO | Decodificação de coleções no Firestore é "tudo ou nada". | `core/data/.../NetworkServiceImpl.kt:126` | `decodeFromJsonElement` é aplicado na lista inteira. Se um único produto ou lista tiver um campo com formato inválido, a coleção inteira retorna `null` e a tela fica vazia. | Desserializar elemento a elemento com `runCatching`, descartando e logando individualmente itens inválidos. |
| **B-08** | P1 | CONFIRMADO | Snapshot listener fecha permanentemente no primeiro erro transitório. | `core/data/.../FirestoreClient.kt:222, 240` | `close(error)` encerra o `callbackFlow` e o repositório emite `emptyList()`. Uma queda rápida de Wi-Fi esvazia a tela e ela nunca mais atualiza até reiniciar. | Implementar reconexão com `retryWhen` / backoff no listener do Firestore. |
| **B-09** | P2 | CONFIRMADO | Tool `compare_prices` expõe preços fictícios simulados como se fossem reais. | `feature/products/.../PriceComparisonUseCase.kt:28-36` | O usuário pergunta "onde o arroz está mais barato?" e a IA responde afirmando preços de supermercados que foram hardcoded em código. | Remover a tool do registry de IA até que haja integração com serviço real de cotação. |
| **B-10** | P2 | CONFIRMADO | Tool `delete_multi_shopping_list` não valida permissão OWNER da lista. | `feature/shopping/.../ShoppingMultiDeleteUseCase.kt:22-38` | Um colaborador com permissão de EDITOR pode pedir no chat para excluir a lista inteira, e a operação é executada sem confirmação. | Remover a tool redundante (ou exigir validação estrita de OWNER). |
| **B-11** | P2 | CONFIRMADO | `ShoppingJoinUseCase` renotifica todos os membros se o usuário já estiver na lista. | `feature/shopping/.../ShoppingJoinUseCase.kt:34-40` | Usuário toca no link de convite de uma lista que já participa → dispara notificação `list_joined` para todos os membros novamente (spam). | Verificar se `shopping.users.contains(user.id)` antes de disparar notificações de boas-vindas. |
| **B-12** | P2 | CONFIRMADO | Cesta básica não permite esvaziamento permanente. | `feature/products/.../CommonProductRepositoryImpl.kt:58-72` | Se o usuário apagar todos os itens da sua lista de produtos comuns, na próxima inicialização `current.isNullOrEmpty()` dispara e recria os 16 itens padrão novamente. | Salvar flag `has_seeded_common_products` no DataStore após o primeiro provisionamento. |
| **B-13** | P2 | CONFIRMADO | N+1 queries de rede ao salvar múltiplos produtos em lote. | `feature/products/.../ProductRepositoryImpl.kt:274-279` | `saveProduct` chama `checkIsLocked` individualmente para cada item. Adicionar 20 produtos executa 20 leituras sequenciais de `shopping/{id}` no Firestore. | Validar `checkIsLocked` uma única vez no início da operação em lote. |

---

### 2.3 Funcionalidades Faltantes e Lacunas de UX (F)

| ID | Sev. | Lacuna | Evidência (`arquivo:linha`) | Proposta de Solução |
|---|---|---|---|---|
| **F-01** | P1 | Orçamento não é exibido no carrinho de compras. | `feature/cart/.../CartScreen.kt` | Implementar barra de progresso de orçamento e indicador visual caso o total ultrapasse o orçamento estipulado na lista. |
| **F-02** | P1 | Falta interface para sair da lista e gerenciar colaboradores. | `feature/shopping/.../ShoppingRepositoryImpl.kt:148` (`leave` sem chamador na UI) | Adicionar opções de menu: "Sair da lista" (para colaboradores) e "Remover membro" (para o dono da lista). |
| **F-03** | P2 | Lembrete diário não possui ação ao ser clicado e ícone desatualizado. | `feature/settings/.../ShoppingReminderWorker.kt:80-92` | Configurar `setContentIntent` apontando para a tela principal e utilizar ícone monocromático compatível com Android 12+. |
| **F-04** | P2 | Ações destrutivas no carrinho (remover item / limpar comprados) não têm "Desfazer". | `feature/cart/.../CartViewModel.kt` | Exibir Snackbar com ação "Desfazer" temporária antes de persistir a exclusão definitiva. |
| **F-05** | P2 | Refresh de status de assinatura PRO não ocorre no ciclo de vida do app. | `feature/subscription/.../PaywallViewModel.kt:40` | Registrar observer de ciclo de vida (`ProcessLifecycleOwner.ON_RESUME`) para invocar `refresh()` e restaurar compras automaticamente. |
| **F-06** | P3 | Módulo `:wear` não possui nenhum teste unitário automatizado. | Diretório `wear/src/test` vazio | Criar suite de testes unitários para ViewModels e decodificação de caminhos de sincronização do Wear. |

---

### 2.4 Infraestrutura, Segurança e Conformidade (D)

| ID | Sev. | Item | Evidência (`arquivo:linha`) | Proposta de Solução |
|---|---|---|---|---|
| **D-01** | **P0** | `firestore.rules` não publicado (regra placeholder em produção). | `firestore.rules:6-14`; `architecture/05` AD-009 | Planejar publicação das regras com validação de compatibilidade para convites e sincronização Wear OS. |
| **D-02** | P3 | Uso de `@Suppress` em código de produção em desacordo com AGENTS.md §10. | `NetworkServiceImpl.kt:116`, `DataStoreStorageService.kt:67`, `ShoppingRepository.kt:7` | Tratar a causa raiz das supressões de cast e complexidade de funções. |

---

## 3. Ordem de Execução Recomendada (Próximos SDDs)

Recomenda-se dividir a implementação nos seguintes SDDs ordenados por impacto:

```
┌────────────────────────────────────────────────────────────────────────┐
│ SDD-016: Billing Hardening & Resiliência de Assinaturas (B-01, C-01, F-05)│
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ SDD-017: AI Agent Safety & Chat Polish (B-02, B-05, B-06, B-09, B-10, C-02, C-03) │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ SDD-018: Firestore Decodificação Resiliente & Sincronização (B-07, B-08)│
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ SDD-019: Shopping Rules & Integridade de Colaboração (B-03, B-04, B-11, B-13, F-02) │
└───────────────────────────────────┬────────────────────────────────────┘
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ SDD-020: Experiência do Carrinho, Orçamento & Lembretes (F-01, F-03, F-04, B-12)    │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Contratos e Interfaces Afetados

Não há novos contratos introduzidos diretamente neste documento de auditoria. Cada SDD derivado detalhará suas interfaces específicas seguindo as diretrizes de visibilidade `internal` por padrão e `public` apenas onde necessário.

---

## 5. Critérios de Aceitação da Auditoria
- [x] Todos os apontamentos contêm evidência concreta (`arquivo:linha`).
- [x] Cada defeito possui cenário claro de reprodução e proposta viável de correção.
- [x] O backlog está livre de referências a itens já corrigidos em sprints anteriores.
- [x] Plano revisado em conformidade com as regras de governança do projeto.
