# Task: Auditoria de Bugs de Lógica Preditivos (App Inteiro)

- **Slug:** auditoria-logica-producao
- **Autor do plano:** Code-Planner (SDD)
- **Data:** 2026-11-01
- **Status:** ready
- **Versão-alvo:** N/A (documento de diagnóstico — correções serão versionadas nos SDDs filhos)
- **Tipo de mudança:** fix (diagnóstico; nenhuma correção aplicada neste documento)
- **Impacto público (módulo):** none

## 1. Contexto e Motivação

Pedido do usuário: revisar o app inteiro e identificar preditivamente **bugs de lógica** (não crashes de framework, não features faltantes, não infra) que podem corromper dados, conceder/negar acesso indevidamente ou produzir comportamento incorreto em produção.

Já existia uma auditoria geral de 2026-10-31 (`.specs/sdd-001-auditoria-geral/tasks.md`), cuja seção 2.2 ("Bugs de Lógica e Integridade de Dados — B") cobria `feature/shopping`, `feature/cart`, `feature/products`, `core/data`, `core/billing`, `feature/chat`. Desde então:
- `B-01`/`C-01` (billing) foram corrigidos no commit `55f2869a` (sdd-016-billing-hardening).
- Os crashes do Crashlytics (CartGraph lifecycle, WorkManager init) foram corrigidos no commit `3b9e3609` (sdd-022), HEAD atual da branch `fix/crashlytics-top-crashes`.
- `sdd-017-ai-agent-safety`, `sdd-018-firestore-resilience`, `sdd-019-shopping-rules-collaboration`, `sdd-020-cart-budget-ux` (referenciados em `.specs/tasks.txt`) **ainda não foram implementados** — nenhuma pasta `.specs/sdd-01[7-9]*` ou `sdd-020*` existe no repositório.

Este documento: (1) **revalida linha a linha** cada item B/C ainda não corrigido contra o código atual; (2) varre módulos com cobertura fraca ou nula nas auditorias anteriores — `core/ai`, `feature/ai-agent`, `core/remote-config`, `core/auth`, `feature/auth`, `feature/profile`, `feature/settings`, `core/navigation`, `wear/`, `app/`; (3) consolida uma lista única priorizada para os SDDs de implementação já planejados.

Metodologia: 4 revisões paralelas e independentes do código em `HEAD` (branch `fix/crashlytics-top-crashes`), cada uma lendo os arquivos reais antes de citar `arquivo:linha`.

---

## 2. Escopo do Diagnóstico

### 2.1 Itens do backlog anterior — status revalidado

| ID | Sev. | Status atual | Evidência (`arquivo:linha`) | Observação |
|---|---|---|---|---|
| **B-01**/**C-01** | — | ✅ **JÁ CORRIGIDO** | `core/billing/.../PlayBillingSubscriptionRepository.kt:107-133,237-295` | `refresh()` só chama `handlePurchases` com `responseCode == OK` (linha 123) e preserva `_status` em qualquer outra resposta; `connectSingleAttempt` tem `withTimeoutOrNull(10s)` + retry com backoff exponencial. Implementação completa do sdd-016. |
| **B-02** | **P0** | 🔴 **CONFIRMADO** | `feature/shopping/.../domain/usecase/ShoppingUpdateUseCase.kt:55-56` | Agente de IA ainda pode sobrescrever/zerar `users`/`roles`. Agravante encontrado nesta rodada: `AgentContext`/metadata passado à tool (`feature/chat/.../state/AiChatUiState.kt:19-22`) **não carrega o `userId` do remetente** — hoje não há nem dado disponível para validar OWNER antes de aplicar a mudança. |
| **B-03** | P1 | 🔴 **CONFIRMADO** | `feature/shopping/.../presentation/viewmodel/ShoppingListViewModel.kt:267-282` | `moveShopping` ainda recalcula `position` sobre a lista filtrada (`mapIndexed` sobre `filteredList`), corrompendo a ordenação real ao arrastar com filtro ativo. |
| **B-04** | P1 | 🔴 **CONFIRMADO** | `feature/shopping/.../presentation/viewmodel/ShoppingListViewModel.kt:236-242` | `applyFilters` só ordena por `NAME`/`createdAt DESC`; não existe `SortingMode.POSITION`. Qualquer emissão do snapshot listener desfaz reordenação manual. |
| **B-05** | P1 | 🔴 **CONFIRMADO** | `feature/chat/.../AiChatViewModel.kt:151` | `markFreeMessageUsed()` ainda é chamado antes do `try/collect` da resposta (linhas 163-187) — crédito grátis é consumido mesmo em falha de rede (`IOException`, catch em 177-181). |
| **B-06** | P1 | 🔴 **CONFIRMADO** | `feature/chat/.../AiChatScreen.kt:296` (e botão de microfone, linha 280) | `enabled = isSendEnabled` nunca combina com `state.isLoading`; `sendMessage()` (`AiChatViewModel.kt:146`) não tem guarda contra chamada concorrente. Múltiplos toques disparam requisições paralelas fora de ordem. |
| **B-07** | P1 | 🔴 **CONFIRMADO** | `core/data/.../network/NetworkServiceImpl.kt:132` | Decodificação ainda é "tudo ou nada": um item malformado no Firestore zera a coleção inteira na UI. |
| **B-08** | P1 | 🔴 **CONFIRMADO** | `core/data/.../network/FirestoreClient.kt:232,250` | `callbackFlow` ainda fecha (`close(error)`) permanentemente no primeiro erro transitório, sem `retryWhen`/backoff. |
| **B-09** | P2 | 🔴 **CONFIRMADO** | `feature/products/.../domain/usecase/PriceComparisonUseCase.kt:10-13,28-35` | Tool `compare_prices` ainda retorna preços fictícios hardcoded apresentados como reais ao usuário via IA. |
| **B-10** | **P0** (reavaliado; era P2 em sdd-001) | 🔴 **CONFIRMADO — severidade reclassificada** | `feature/shopping/.../domain/usecase/ShoppingMultiDeleteUseCase.kt:26-29` | Nenhuma checagem de papel (OWNER) antes de excluir a lista inteira via tool de IA. Reclassificado para **P0** nesta auditoria: é exclusão destrutiva e irreversível de dados de terceiros (demais membros), acionável por qualquer colaborador (até VIEWER) só pedindo no chat, sem confirmação. |
| **B-11** | P2 | 🔴 **CONFIRMADO** | `feature/shopping/.../domain/usecase/ShoppingJoinUseCase.kt:30,37-39` | `join()` e notificação disparam incondicionalmente, mesmo se o usuário já é membro. |
| **B-12** | P2 | 🔴 **CONFIRMADO** | `feature/products/.../data/repository/CommonProductRepositoryImpl.kt:56-66` | `seedDefaultsIfEmpty` recria os 16 itens padrão sempre que a lista fica vazia — esvaziamento intencional não é respeitado. |
| **B-13** | P2 | 🔴 **CONFIRMADO** | `feature/products/.../data/repository/ProductRepositoryImpl.kt:86-88,275-282` + loop em `ProductListSaveUseCase.kt:30-34` | N+1 leituras do Firestore (`checkIsLocked` por item) ao salvar produtos em lote via IA. |
| **C-02** | P1 | 🔴 **CONFIRMADO (ambos providers)** | `feature/ai-agent/.../GeminiAgent.kt:115` e `feature/ai-agent/.../OpenRouterAgent.kt:117-118` | `response.text?.trim()?.let { emit(it) }` sem `else`; se o modelo responde vazio/branco, nada é emitido. Consumidor (`AiChatViewModel.kt:163-174`) não tem `.onCompletion`, então `isLoading` nunca retorna a `false` — spinner trava. |
| **C-03** | P2 | 🟡 **DIVERGENTE** (não crasha mais, mas lógica ainda errada) | `feature/shopping/.../ShoppingUpdateUseCase.kt:52-54` | `Shopping.Status.valueOf(it)` sem tolerância a casing. Hoje é capturado pelo `try/catch` de `GeminiAgent.executeFunctionCall` e retorna erro ao LLM em vez de crashar — mas um `status` minúsculo gramaticalmente válido (ex. `"finished"`) é **rejeitado indevidamente**, é bug de lógica residual. |
| **C-04** | P2 | 🔴 **CONFIRMADO** (reclassificado de crash para integridade de dados) | `feature/products/.../domain/usecase/ProductListSaveUseCase.kt:43-57` | `split(",")` sem filtrar vazios ainda cria `Product(name="")` quando há vírgula duplicada na entrada. |

### 2.2 Bugs de lógica novos encontrados nesta auditoria

| ID | Sev. | Confirmação | Defeito | Evidência (`arquivo:linha`) | Cenário de Falha | Correção Proposta |
|---|---|---|---|---|---|---|
| **B-14** | **P1** | CONFIRMADO | Preferência de moeda do usuário (`AppSettings.currency`, editável em Configurações) nunca é aplicada à formatação de preços — é puramente cosmética. | `core/ui/.../extensions/CurrencyExtensions.kt:29-35` (`rememberCurrencyFormatter()` usa `NumberFormat.getCurrencyInstance()` sem ler `LocalCurrency`); `core/ui/.../utils/LocalCurrency.kt:7-13` (nunca é `provides` em código de produção, só em teste); `feature/settings/.../SettingsViewModel.kt:185-189` (valor só alimenta o subtítulo da própria tela de Settings). | Usuário com dispositivo em locale `en-US`/moeda `USD` escolhe "Real brasileiro (BRL)" em Configurações → Moeda. O subtítulo da tela muda, mas todo preço em `CartScreen`, `CartBottomBar`, `EditItemContent`, `ConfirmItemContent`, `FinishPurchaseContent`, `ProductForm`, `EditShoppingContent`, `ShoppingSummaryCard`, `ShoppingItem`, `CartProductItem` e na tela do Wear (`ShoppingDetailScreen.kt:128-208`) continua em USD. | Prover `LocalCurrency` (consumido dentro de `rememberCurrencyFormatter()`) a partir de `AppSettings.currency` no ponto onde o estado de settings já é observado globalmente (ex.: raiz do tema em `MainActivity`), em vez de depender apenas do locale do dispositivo. |
| **B-15** | P2 | NOVO | `AgentFactoryImpl` cai silenciosamente para a chave compartilhada do app quando o tier é BYOK mas a chave do usuário está em branco, sem avisar a UI. | `feature/ai-agent/.../AgentFactoryImpl.kt:35-42,62-66` | Usuário seleciona tier BYOK mas a chave fica vazia (erro de digitação/salvamento na UI de configurações) → app usa a cota gratuita compartilhada em nome do usuário, sem nenhum aviso de que o BYOK configurado não está em uso. | Emitir estado de erro/aviso explícito na UI de configurações de IA quando `aiConfigTier == CUSTOM_BYOK` e `customApiKey` estiver vazio, em vez de fallback silencioso dentro da factory. |
| **B-16** | P2 | NOVO | `ProfileViewModel.observeProfile()` lê `authService.currentUser` como snapshot síncrono único no `init`; se vier `null` por corrida com a propagação assíncrona do `userId` pós-login, a tela de Perfil nunca se inscreve em `getUserProfile`. | `feature/profile/.../ProfileViewModel.kt:46-54`; corrida com `FirebaseAuthService.kt:70-74` (`storage.observe<String>(USER_ID_KEY)` em coroutine separada). | Usuário faz login e navega para Perfil antes do `USER_ID_KEY` ser propagado via DataStore → `currentUser` ainda `null` → tela de Perfil permanece em branco/estado default enquanto essa instância do ViewModel existir, mesmo após o login completar. | Coletar `authService.authState` (Flow) no `init` e, a cada emissão não-nula, (re)disparar a inscrição em `getUserProfile` com `collectLatest`/`flatMapLatest`, cancelando a anterior. |
| **B-17** | P2 (confiança média — não validado em runtime) | NOVO | `ShoppingReminderScheduler.sync()` sempre usa `ExistingPeriodicWorkPolicy.UPDATE` sem `cancelUniqueWork` prévio; a semântica do WorkManager para `UPDATE` não garante que um novo `initialDelay` seja respeitado após a primeira execução do worker. | `feature/settings/.../data/worker/ShoppingReminderScheduler.kt:45-53` | Usuário muda o horário do lembrete de 20h para 08h depois que o worker diário já disparou pelo menos uma vez → o próximo disparo pode continuar seguindo o ciclo antigo (20h) em vez do novo horário, dependendo da versão do WorkManager. | Chamar `cancelUniqueWork(WORK_NAME)` antes de `enqueueUniquePeriodicWork` ao detectar mudança de horário; validar com teste instrumentado (`WorkManager` `TestDriver`) antes de fechar o item — **bloqueante para confirmar severidade real**. |

### 2.3 Áreas revisadas sem bugs de lógica novos

- `core/billing` (além de B-01/C-01, já corrigido): cache, acknowledge, mapeamento de status — sem novos problemas.
- `core/auth`, `feature/auth`: fluxo de sign-in/sign-out/delete-account sem bug de lógica (gap de compliance em `deleteAccount()` não limpar dados Firestore é lacuna de funcionalidade, não lógica — já coberto por D-01 em `sdd-001`).
- `core/remote-config`: leitura de flags, `VALUE_SOURCE_STATIC`, JSON vs boolean — sem bugs.
- `feature/ai-agent` (`AiAgentLimits`, `FallbackAgent`): já corrigidos por sdd-006/sdd-012, nada novo.
- `wear/` (`WearListStore`, `WearAuthListenerService`, `LastListTileService`): sem condição de corrida — Wear é consumidor single-writer simples, sem merge bidirecional.
- `app/` (`CestouApplication`, `WorkManagerSafeInit`): guarda do fix de crash (`3b9e3609`) está logicamente completa, não esconde agendamento duplicado/perdido.
- `core/common` (parsing de data/hora): seguro desde sdd-005 (C-02 original).
- `core/navigation`: nenhum bug de lógica; nota lateral não-bloqueante — `Navigator.kt:61-65` tem um `if/else` com ramos idênticos (código morto, sem efeito de comportamento, candidato a limpeza em outro SDD de simplificação).

---

## 3. Ordem de Execução Recomendada (Próximos SDDs)

Atualiza o roadmap de `.specs/tasks.txt` com a reclassificação de severidade (B-10 → P0) e os itens novos (B-14 a B-17):

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ SDD-017: AI Agent Safety & Chat Polish                                       │
│ P0: B-02 (+ plumbing de userId no metadata da tool) · B-10 (reclassificado)  │
│ P1: B-05 · B-06 · C-02                                                       │
│ P2: C-03 · B-09 · B-15                                                       │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ SDD-018: Firestore Decodificação Resiliente & Sincronização (B-07, B-08)     │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ SDD-019: Shopping Rules & Integridade de Colaboração                        │
│ B-03 · B-04 · B-11 · B-13 · B-16 (profile) · B-17 (reminder, validar antes)  │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ SDD-020: Carrinho, Orçamento, Moeda & Lembretes                             │
│ B-12 · C-04 (nome vazio) · B-14 (moeda não aplicada — maior escopo de UI)    │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Recomendação de prioridade imediata:** B-02 e B-10 (ambos P0, exploráveis via chat de IA por qualquer colaborador, resultam em perda de dados/posse) devem ser corrigidos antes de qualquer outro item deste backlog — ambos compartilham a mesma causa raiz estrutural: o contrato de metadata passado da tool de IA para os use cases não carrega o `userId` do remetente, impedindo qualquer validação de permissão. Resolver esse plumbing uma vez beneficia os dois itens.

---

## 4. Contratos e Interfaces Afetados

Nenhum contrato é alterado diretamente neste documento de auditoria. Nota de atenção para o SDD-017: a correção de B-02/B-10 provavelmente exigirá adicionar `userId` (ou `callerId`) ao contrato de metadata/contexto hoje passado às tools de IA (`AiChatUiState.kt:19-22` e equivalentes em `GeminiAgent`/`OpenRouterAgent`) — **marcar como `public` dentro do módulo `core:ai`/`feature:ai-agent`** se consumido por múltiplas features, com justificativa de que use cases de outras features (`shopping`, `products`) precisam ler esse valor para validar permissão.

---

## 5. Regras de Negócio Aplicáveis

| # | Regra atual | Comportamento esperado | Origem (`arquivo:linha`) |
|---|---|---|---|
| 1 | Qualquer membro pode alterar `users`/`roles` de uma lista via IA. | Somente o OWNER pode alterar `users`/`roles`. | `ShoppingUpdateUseCase.kt:55-56` |
| 2 | Qualquer membro pode excluir a lista inteira via IA (tool `delete_multi_shopping_list`). | Somente o OWNER pode excluir a lista. | `ShoppingMultiDeleteUseCase.kt:26-29` |
| 3 | Crédito de mensagem grátis é debitado antes do envio ter sucesso. | Crédito só deve ser debitado após resposta bem-sucedida. | `AiChatViewModel.kt:151` |
| 4 | Reordenação de lista (`moveShopping`) opera sobre o array filtrado exibido na tela. | Deve operar sobre a lista completa, preservando índices absolutos. | `ShoppingListViewModel.kt:267-282` |
| 5 | Preferência de moeda do usuário não é lida na formatação de preços. | Toda formatação monetária deve refletir `AppSettings.currency`. | `CurrencyExtensions.kt:29-35` |

---

## 6. Critérios de Aceitação da Auditoria
- [x] Todos os apontamentos contêm evidência concreta (`arquivo:linha`) lida do código em `HEAD`.
- [x] Itens do backlog anterior (`sdd-001`) foram revalidados individualmente (corrigido / confirmado / divergente).
- [x] Severidades foram reavaliadas quando a evidência justificou (B-10: P2→P0).
- [x] Novos achados (B-14 a B-17) incluem cenário de falha concreto e proposta de correção.
- [x] Nenhum código de produção foi alterado nesta tarefa.
- [ ] B-17 requer validação em runtime (teste instrumentado de WorkManager) antes de entrar em implementação — não bloqueante para o restante do documento.

---

## 7. Plano de Testes (para os SDDs de implementação)

- **B-02/B-10 (SDD-017):** teste unitário de `ShoppingUpdateUseCase`/`ShoppingMultiDeleteUseCase` simulando chamador não-OWNER → deve retornar erro/negar, não executar. Teste de regressão para o plumbing de `userId` no metadata da tool.
- **B-05/B-06 (SDD-017):** teste unitário de `AiChatViewModel` garantindo que `markFreeMessageUsed()` só roda após sucesso; teste de UI (`testTag`) garantindo botão de envio desabilitado durante `isLoading`.
- **C-02 (SDD-017):** teste unitário dos providers garantindo emissão de fallback quando `text` vier nulo/branco, e `isLoading=false` ao final do `collect`.
- **B-07/B-08 (SDD-018):** teste unitário de decodificação item-a-item com um elemento inválido na coleção; teste de `FirestoreClient` simulando erro transitório seguido de recuperação.
- **B-03/B-04 (SDD-019):** teste unitário de `moveShopping` com filtro ativo, validando posições absolutas preservadas.
- **B-14 (SDD-020):** teste unitário/instrumentado garantindo que `rememberCurrencyFormatter()` reflete `AppSettings.currency` após alteração nas Configurações.
- **B-17 (SDD-019):** teste instrumentado com `WorkManager` `TestDriver` validando reagendamento após mudança de horário pós-primeira execução — pré-requisito antes de decidir a correção.
- Lint/Detekt/Cobertura: `./gradlew lintDebug detekt testDebug koverVerify koverVerifyLayout` em cada SDD filho.

---

## 8. Dependências e Riscos

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| B-02/B-10 exploráveis em produção hoje (qualquer colaborador pode zerar posse ou excluir lista via chat) | Alta (basta pedir no chat) | Perda de dados/posse de terceiros | Priorizar SDD-017 antes de qualquer outro item deste backlog |
| B-17 pode não ser reproduzível (comportamento do WorkManager `UPDATE` varia por versão/dispositivo) | Média | Lembrete chega no horário errado por até 1 ciclo | Validar com teste instrumentado antes de implementar correção |
| Falta de regras do Firestore publicadas (`D-01`, `sdd-001`) remove a segunda camada de defesa para B-02/B-10 | Alta | Mesmo corrigindo a lógica do app, cliente modificado poderia repetir o ataque direto ao Firestore | Tratar `D-01` em paralelo ou antes do SDD-017 |

---

## 9. Perguntas em Aberto (bloqueantes)

Nenhuma bloqueante para a publicação deste diagnóstico. Não bloqueante: validar B-17 em runtime antes de priorizá-lo (ver seção 6).

## 10. Checklist de Conformidade
- [x] Decisões citam `arquivo:linha` (e `.specs/sdd-001-auditoria-geral` como origem, quando aplicável).
- [x] Nenhum código de produção escrito (apenas diagnóstico).
- [x] Seção 6 preenchida de forma substantiva.
- [x] Nenhuma mudança em símbolo público proposta diretamente (apenas nota de atenção para o SDD-017).
- [x] Perguntas em aberto exauridas (nenhuma bloqueante).
